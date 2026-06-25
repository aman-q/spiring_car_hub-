package com.carhub.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed access-token blacklist. On logout a token's {@code jti} is stored
 * under {@code bl:<jti>} with a TTL equal to the token's remaining lifetime, so the
 * entry expires exactly when the token would have anyway.
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String PREFIX = "bl:";

    private final StringRedisTemplate redis;

    public void blacklist(String jti, Duration ttl) {
        if (jti != null && !ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(PREFIX + jti, "1", ttl);
        }
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
