package com.carhub.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Rate-limit configuration, bound from {@code carhub.rate-limit.*}.
 *
 * <p>{@code global} is applied to every request; {@code policies} are keyed by the
 * name referenced in {@link com.carhub.ratelimit.RateLimit#policy()} on a handler.
 * Keeping limits here (not as annotation literals) makes them tunable per environment.
 */
@ConfigurationProperties(prefix = "carhub.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Policy global,
        Map<String, Policy> policies
) {

    public record Policy(String keyPrefix, int limit, long windowSeconds) {
    }

    public Policy policy(String name) {
        return policies == null ? null : policies.get(name);
    }
}
