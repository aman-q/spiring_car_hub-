package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT signing + lifetime configuration, bound from {@code carhub.jwt.*}.
 */
@ConfigurationProperties(prefix = "carhub.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
