package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-agnostic email settings, bound from {@code carhub.email.*}. {@code provider}
 * selects the active {@link com.carhub.email.EmailProvider} implementation.
 */
@ConfigurationProperties(prefix = "carhub.email")
public record EmailProperties(
        String provider,
        String senderName,
        String senderEmail
) {
}
