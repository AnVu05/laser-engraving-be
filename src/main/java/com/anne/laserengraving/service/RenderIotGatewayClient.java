package com.anne.laserengraving.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class RenderIotGatewayClient {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final String secretKey;
    private final boolean enabled;

    public RenderIotGatewayClient(
            @Value("${app.render.gateway-url:http://localhost:8081}") String gatewayUrl,
            @Value("${app.render.secret-key:laser-internal-secret-key-change-me}") String secretKey,
            @Value("${app.render.enabled:true}") boolean enabled) {
        this.gatewayUrl = gatewayUrl.endsWith("/") ? gatewayUrl.substring(0, gatewayUrl.length() - 1) : gatewayUrl;
        this.secretKey = secretKey;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);

        log.info("[RENDER GATEWAY CLIENT] Khởi tạo Client: url={}, enabled={}", this.gatewayUrl, this.enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Lấy thông tin chi tiết một Job từ Render Gateway (trạng thái, tiến độ thực tế từ bo mạch)
     */
    public Optional<Map<String, Object>> getJobDetail(String jobCode) {
        if (!enabled) return Optional.empty();

        String url = gatewayUrl + "/api/v1/internal/jobs/" + jobCode;
        HttpHeaders headers = createAuthHeaders();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable((Map<String, Object>) response.getBody().get("data"));
            }
        } catch (RestClientException e) {
            log.debug("[RENDER GATEWAY] Lỗi lấy thông tin Job {} từ Render: {}", jobCode, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Đẩy Job và nội dung G-code sang Render IoT Gateway
     */
    public Optional<Map<String, Object>> dispatchJobToGateway(
            String jobCode, String deviceCode, String filename, String gcodeContent,
            Double widthMm, Double heightMm, Integer feedRate, Integer laserPower, String materialType) {

        if (!enabled) {
            log.debug("[RENDER GATEWAY] Gateway đang bị tắt (enabled=false); bỏ qua chuyển tiếp Job {}", jobCode);
            return Optional.empty();
        }

        String url = gatewayUrl + "/api/v1/internal/jobs/dispatch";
        HttpHeaders headers = createAuthHeaders();

        Map<String, Object> body = Map.of(
                "jobCode", jobCode,
                "deviceCode", deviceCode,
                "originalFilename", filename != null ? filename : jobCode + ".gcode",
                "gcodeContent", gcodeContent,
                "targetWidthMm", widthMm != null ? widthMm : 100.0,
                "targetHeightMm", heightMm != null ? heightMm : 100.0,
                "feedRate", feedRate != null ? feedRate : 1000,
                "laserPower", laserPower != null ? laserPower : 500,
                "materialType", materialType != null ? materialType : "Standard"
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("[RENDER GATEWAY] Đã chuyển tiếp Job {} sang Render Gateway thành công", jobCode);
                return Optional.ofNullable((Map<String, Object>) response.getBody().get("data"));
            }
        } catch (RestClientException e) {
            log.warn("[RENDER GATEWAY] Không thể kết nối tới Render Gateway ({}) khi dispatch Job {}: {}",
                    url, jobCode, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Lấy Telemetry mới nhất (tiến độ, toạ độ X/Y, cảm biến) từ Render Gateway
     */
    public Optional<Map<String, Object>> getDeviceTelemetry(String deviceCode) {
        if (!enabled) return Optional.empty();

        String url = gatewayUrl + "/api/v1/internal/devices/" + deviceCode + "/telemetry";
        HttpHeaders headers = createAuthHeaders();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable((Map<String, Object>) response.getBody().get("data"));
            }
        } catch (RestClientException e) {
            log.warn("[RENDER GATEWAY] Lỗi lấy Telemetry của máy {} từ Render: {}", deviceCode, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Lấy trạng thái hoạt động (ONLINE/OFFLINE/BUSY) của máy từ Render Gateway
     */
    public Optional<Map<String, Object>> getDeviceStatus(String deviceCode) {
        if (!enabled) return Optional.empty();

        String url = gatewayUrl + "/api/v1/internal/devices/" + deviceCode + "/status";
        HttpHeaders headers = createAuthHeaders();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable((Map<String, Object>) response.getBody().get("data"));
            }
        } catch (RestClientException e) {
            log.warn("[RENDER GATEWAY] Lỗi lấy trạng thái của máy {} từ Render: {}", deviceCode, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Gửi lệnh tức thời (PAUSE, RESUME, CANCEL, HOME) sang Render Gateway để bắn xuống bo mạch
     */
    public boolean sendDeviceCommand(String deviceCode, String action, String jobCode, String reason) {
        if (!enabled) return false;

        String url = gatewayUrl + "/api/v1/internal/devices/" + deviceCode + "/commands";
        HttpHeaders headers = createAuthHeaders();

        Map<String, Object> body = Map.of(
                "action", action,
                "jobCode", jobCode != null ? jobCode : "",
                "reason", reason != null ? reason : ""
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("[RENDER GATEWAY] Lỗi gửi lệnh {} xuống máy {} qua Render: {}", action, deviceCode, e.getMessage());
            return false;
        }
    }

    /**
     * Đồng bộ cấu hình giới hạn bàn máy sang Render Gateway
     */
    public boolean syncDeviceConfig(String deviceCode, Map<String, Object> configPayload) {
        if (!enabled) return false;

        String url = gatewayUrl + "/api/v1/internal/devices/" + deviceCode + "/config";
        HttpHeaders headers = createAuthHeaders();

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(configPayload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("[RENDER GATEWAY] Lỗi đồng bộ cấu hình máy {} qua Render: {}", deviceCode, e.getMessage());
            return false;
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret-Key", secretKey);
        return headers;
    }
}
