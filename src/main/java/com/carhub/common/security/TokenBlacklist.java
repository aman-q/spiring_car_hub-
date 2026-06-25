package com.carhub.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed access-token blacklist. On logout a token's {@code jti} is stored
 * under {@code bl:<jti>} with a TTL equal to the token's remaining lifetime, so the
 * entry expires exactly when the token would have anyway.
 *
 * <p>Reads fail open: if Redis is unreachable {@link #isBlacklisted} returns
 * {@code false} so a cache outage doesn't reject every authenticated request. Short-
 * lived access tokens bound the exposure window.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String PREFIX = "bl:";

    private final StringRedisTemplate redis;

    public void blacklist(String jti, Duration ttl) {
        if (jti != null && !ttl.isNegative() && !ttl.isZero()) {
            try {
                redis.opsForValue().set(PREFIX + jti, "1", ttl);
            } catch (DataAccessException e) {
                log.warn("Failed to blacklist token {}: {}", jti, e.getMessage());
            }
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
        } catch (DataAccessException e) {
            log.warn("Blacklist check unavailable, treating token as active: {}", e.getMessage());
            return false;
        }
    }
}
