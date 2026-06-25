package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brevo transactional-email API settings, bound from {@code carhub.brevo.*}.
 * Sender identity is shared across providers via {@link EmailProperties}.
 */
@ConfigurationProperties(prefix = "carhub.brevo")
public record BrevoProperties(
        String apiKey,
        String baseUrl
) {
}
