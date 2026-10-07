package com.anne.laserengraving.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tiện ích quản lý HttpOnly Cookie chứa JWT Token nhằm ngăn ngừa tấn công XSS (Cross-Site Scripting).
 */
@Component
public class CookieUtil {

    public static final String AUTH_COOKIE_NAME = "laser_auth_token";

    private final long expirationMs;
    private final boolean secureCookie;

    public CookieUtil(
            @Value("${app.jwt.expiration-ms:604800000}") long expirationMs,
            @Value("${app.jwt.cookie-secure:false}") boolean secureCookie
    ) {
        this.expirationMs = expirationMs;
        this.secureCookie = secureCookie;
    }

    /**
     * Tạo HttpOnly Cookie chứa JWT token an toàn với thời gian sống xác định.
     */
    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite("Lax")
                .build();
    }

    /**
     * Tạo HttpOnly Cookie xoá phiên đăng nhập (Max-Age = 0).
     */
    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /**
     * Trích xuất JWT Token từ HttpOnly Cookie trong request.
     */
    public String extractToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
