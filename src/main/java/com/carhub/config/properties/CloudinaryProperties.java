package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudinary credentials + default upload folder, bound from {@code carhub.cloudinary.*}.
 */
@ConfigurationProperties(prefix = "carhub.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder
) {
}
