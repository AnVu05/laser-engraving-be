package com.anne.laserengraving.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CookieJwtAuthenticationFilter cookieJwtAuthenticationFilter;
    private final String allowedOrigins;

    public SecurityConfig(CookieJwtAuthenticationFilter cookieJwtAuthenticationFilter,
                          @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000}") String allowedOrigins) {
        this.cookieJwtAuthenticationFilter = cookieJwtAuthenticationFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"success\":false,\"message\":\"Yêu cầu đăng nhập tài khoản Quản trị viên (ADMIN) để thực hiện thao tác này!\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền quản trị (ADMIN/OPERATOR) để truy cập tài nguyên này!\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints cho bo mạch phần cứng MKS DLC32 (Xác thực nội bộ qua Bearer token phần cứng & X-Device-Code)
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*/download").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/code/*/download").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/devices/heartbeat").permitAll()

                        // 2. Endpoints Quản trị & Điều hành Máy Laser (BẮT BUỘC ROLE ADMIN HOẶC OPERATOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/confirm").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/cancel").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/devices/*/config").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/devices/*/token").hasAnyRole("ADMIN", "OPERATOR")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/settings").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/materials/init-defaults").hasAnyRole("ADMIN", "OPERATOR")

                        // 3. Endpoints Công khai, Xác thực & Studio Designer
                        .requestMatchers(HttpMethod.GET, "/api/v1/devices/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/devices/*/commands").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/command").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/dispatch").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*/telemetry").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/code/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/materials/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/materials").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/materials/*").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/materials/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings").permitAll()
                        .requestMatchers("/api/v1/gcode/**").permitAll()
                        .requestMatchers("/api/v1/history/**").permitAll()
                        .requestMatchers("/api/v1/projects/**").permitAll()

                        // 4. WebSocket, Swagger Docs & H2 Console
                        .requestMatchers("/ws/**", "/ws/device/**", "/ws/client/**").permitAll()
                        .requestMatchers("/h2-console/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()

                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .addFilterBefore(cookieJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
