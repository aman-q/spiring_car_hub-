package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration, bound from {@code carhub.cors.*}. Defaults are permissive for
 * local dev; production should set an explicit allow-list via {@code CORS_ALLOWED_ORIGINS}.
 */
@ConfigurationProperties(prefix = "carhub.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders
) {

    public List<String> allowedOrigins() {
        return (allowedOrigins == null || allowedOrigins.isEmpty()) ? List.of("*") : allowedOrigins;
    }

    public List<String> allowedMethods() {
        return (allowedMethods == null || allowedMethods.isEmpty())
                ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                : allowedMethods;
    }

    public List<String> allowedHeaders() {
        return (allowedHeaders == null || allowedHeaders.isEmpty())
                ? List.of("Content-Type", "Authorization")
                : allowedHeaders;
    }
}
