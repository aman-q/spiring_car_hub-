package com.carhub.ratelimit;

import com.carhub.abuselog.AbuseLog;
import com.carhub.abuselog.AbuseLogRepository;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.config.properties.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Core fixed-window limiter over Redis (INCR + EXPIRE + TTL), mirroring the Node
 * {@code rateLimiter.js}. Sets {@code X-RateLimit-*} headers, persists an
 * {@link AbuseLog} on violation, and throws {@link ErrorCode#RATE_LIMIT_EXCEEDED}.
 *
 * <p><b>Fail-open:</b> if Redis is unreachable the limiter logs and allows the request
 * rather than 500-ing the whole API — availability is preferred over strict limiting
 * during an infrastructure outage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final AbuseLogRepository abuseLogRepository;

    public void enforce(HttpServletRequest request, HttpServletResponse response, RateLimitProperties.Policy policy) {
        String clientIp = clientIp(request);
        String key = policy.keyPrefix() + clientIp;

        long current;
        long retryAfter;
        try {
            Long incremented = redis.opsForValue().increment(key);
            current = (incremented == null) ? 1L : incremented;
            if (current == 1L) {
                redis.expire(key, Duration.ofSeconds(policy.windowSeconds()));
            }
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            retryAfter = (ttl == null || ttl < 0) ? policy.windowSeconds() : ttl;
        } catch (DataAccessException e) {
            // Redis down — fail open so a cache outage doesn't take down the API.
            log.warn("Rate limiter unavailable, allowing request [{} {}]: {}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(policy.limit() - current, 0)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfter));

        if (current > policy.limit()) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            persistAbuseLog(clientIp, request, current, policy);
            throw new ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, retryAfter);
        }
    }

    private void persistAbuseLog(String clientIp, HttpServletRequest request, long current,
                                 RateLimitProperties.Policy policy) {
        try {
            abuseLogRepository.save(AbuseLog.builder()
                    .ipAddress(clientIp)
                    .route(request.getRequestURI())
                    .method(request.getMethod())
                    .exceededLimit(current)
                    .allowedLimit(policy.limit())
                    .windowSec(policy.windowSeconds())
                    .createdAt(Instant.now())
                    .build());
        } catch (DataAccessException e) {
            // Auditing the violation must never mask the 429 we're about to return.
            log.warn("Failed to persist abuse log for {}: {}", clientIp, e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
