package com.anne.laserengraving.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích tạo và xác thực JSON Web Token (JWT) chuẩn RFC 7519 với thuật toán HMAC-SHA256.
 * Không phụ thuộc thư viện bên ngoài, tích hợp bảo mật an toàn với HttpOnly Cookie.
 */
@Slf4j
@Component
public class JwtUtil {

    private final String secretKey;
    private final long expirationMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtUtil(
            @Value("${app.jwt.secret:laser-engraving-secure-jwt-secret-key-production-ready-2026-ojt-token}") String secretKey,
            @Value("${app.jwt.expiration-ms:604800000}") long expirationMs
    ) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    /**
     * Tạo JWT Token chứa email, userId và vai trò người dùng (hiệu lực mặc định 7 ngày).
     */
    public String generateToken(String email, Integer userId, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expirationMs / 1000);

            // Header
            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put("alg", "HS256");
            headerMap.put("typ", "JWT");
            String headerJson = objectMapper.writeValueAsString(headerMap);
            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

            // Payload
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sub", email);
            payloadMap.put("userId", userId);
            payloadMap.put("role", role);
            payloadMap.put("iat", now);
            payloadMap.put("exp", exp);
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Signature
            String contentToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(contentToSign, secretKey);

            return contentToSign + "." + signature;
        } catch (Exception e) {
            log.error("[JWT] Lỗi khi tạo JWT token cho {}: {}", email, e.getMessage());
            throw new IllegalStateException("Không thể tạo JWT token", e);
        }
    }

    /**
     * Kiểm tra tính hợp lệ và thời hạn của JWT Token.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String contentToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(contentToSign, secretKey);
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("[JWT] Chữ ký Token không hợp lệ!");
                return false;
            }

            // Kiểm tra hạn sử dụng (exp)
            byte[] payloadBytes = base64UrlDecode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);
            if (!payloadNode.has("exp")) {
                return false;
            }
            long exp = payloadNode.get("exp").asLong();
            long now = Instant.now().getEpochSecond();
            return now < exp;
        } catch (Exception e) {
            log.warn("[JWT] Xác thực Token thất bại: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Trích xuất email (Subject) từ JWT Token.
     */
    public String extractEmail(String token) {
        try {
            JsonNode payload = parsePayload(token);
            if (payload != null && payload.has("sub")) {
                return payload.get("sub").asText();
            }
        } catch (Exception e) {
            log.warn("[JWT] Không thể trích xuất email: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Trích xuất userId từ JWT Token.
     */
    public Integer extractUserId(String token) {
        try {
            JsonNode payload = parsePayload(token);
            if (payload != null && payload.has("userId") && !payload.get("userId").isNull()) {
                return payload.get("userId").asInt();
            }
        } catch (Exception e) {
            log.warn("[JWT] Không thể trích xuất userId: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Trích xuất vai trò (Role) từ JWT Token.
     */
    public String extractRole(String token) {
        try {
            JsonNode payload = parsePayload(token);
            if (payload != null && payload.has("role")) {
                return payload.get("role").asText();
            }
        } catch (Exception e) {
            log.warn("[JWT] Không thể trích xuất role: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode parsePayload(String token) throws Exception {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        byte[] payloadBytes = base64UrlDecode(parts[1]);
        return objectMapper.readTree(payloadBytes);
    }

    private String sign(String data, String key) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] signatureBytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signatureBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
