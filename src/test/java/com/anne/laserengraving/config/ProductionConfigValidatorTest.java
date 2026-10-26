package com.anne.laserengraving.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigValidatorTest {
    @Test
    void acceptsStrongSecretAndExactHttpsOrigins() {
        ProductionConfigValidator validator = new ProductionConfigValidator(
                "a-strong-production-secret-with-at-least-32-bytes",
                "https://app.example.com, https://admin.example.com:8443");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void rejectsShortSecret() {
        ProductionConfigValidator validator = new ProductionConfigValidator("too-short", "https://app.example.com");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void rejectsWildcardOrNonHttpsCorsOrigin() {
        ProductionConfigValidator wildcard = new ProductionConfigValidator(
                "a-strong-production-secret-with-at-least-32-bytes", "https://*");
        ProductionConfigValidator http = new ProductionConfigValidator(
                "a-strong-production-secret-with-at-least-32-bytes", "http://app.example.com");

        assertThrows(IllegalStateException.class, () -> wildcard.run(null));
        assertThrows(IllegalStateException.class, () -> http.run(null));
    }
}
