package com.carhub.ratelimit;

import com.carhub.abuselog.AbuseLog;
import com.carhub.abuselog.AbuseLogRepository;
import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.config.properties.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Core fixed-window limiter over Redis (INCR + EXPIRE + TTL), mirroring the Node
 * {@code rateLimiter.js}. Sets {@code X-RateLimit-*} headers, persists an
 * {@link AbuseLog} on violation, and throws {@link ErrorCode#RATE_LIMIT_EXCEEDED}.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final AbuseLogRepository abuseLogRepository;

    public void enforce(HttpServletRequest request, HttpServletResponse response, RateLimitProperties.Policy policy) {
        String clientIp = clientIp(request);
        String key = policy.keyPrefix() + clientIp;

        Long current = redis.opsForValue().increment(key);
        if (current == null) {
            current = 1L;
        }
        if (current == 1L) {
            redis.expire(key, Duration.ofSeconds(policy.windowSeconds()));
        }

        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        long retryAfter = (ttl == null || ttl < 0) ? policy.windowSeconds() : ttl;

        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(policy.limit() - current, 0)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfter));

        if (current > policy.limit()) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            abuseLogRepository.save(AbuseLog.builder()
                    .ipAddress(clientIp)
                    .route(request.getRequestURI())
                    .method(request.getMethod())
                    .exceededLimit(current)
                    .allowedLimit(policy.limit())
                    .windowSec(policy.windowSeconds())
                    .createdAt(Instant.now())
                    .build());
            throw new ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, retryAfter);
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
