package com.anne.laserengraving.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Arrays;

@Component
@Profile("prod")
@Slf4j
public class ProductionConfigValidator implements ApplicationRunner {
    private final String jwtSecret;
    private final String corsOrigins;

    public ProductionConfigValidator(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.cors.allowed-origins}") String corsOrigins) {
        this.jwtSecret = jwtSecret;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32
                || jwtSecret.contains("local-development-only")) {
            throw new IllegalStateException("Production requires APP_JWT_SECRET with at least 32 UTF-8 bytes.");
        }
        boolean validOrigins = !corsOrigins.isBlank() && Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .allMatch(ProductionConfigValidator::isExactHttpsOrigin);
        if (!validOrigins) {
            throw new IllegalStateException("Production APP_CORS_ALLOWED_ORIGINS must contain exact HTTPS origins only.");
        }
        log.info("Production configuration validated: secure cookies enabled; CORS origins are explicit HTTPS origins.");
    }

    private static boolean isExactHttpsOrigin(String value) {
        try {
            URI origin = URI.create(value);
            return "https".equalsIgnoreCase(origin.getScheme())
                    && origin.getHost() != null
                    && origin.getRawUserInfo() == null
                    && (origin.getRawPath() == null || origin.getRawPath().isEmpty())
                    && origin.getRawQuery() == null
                    && origin.getRawFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
