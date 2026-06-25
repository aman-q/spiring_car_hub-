package com.carhub.ratelimit;

import com.carhub.config.properties.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Enforces per-route limits on handlers annotated with {@link RateLimit}, before
 * the controller method runs. Replaces the route-specific Express middlewares
 * ({@code loginRateLimiter}, {@code registerRateLimiter}, {@code otpRateLimiter}).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    @Before("@annotation(rateLimit)")
    public void enforce(RateLimit rateLimit) {
        if (!properties.enabled()) {
            return;
        }
        RateLimitProperties.Policy policy = properties.policy(rateLimit.policy());
        if (policy == null) {
            return;
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        rateLimitService.enforce(attrs.getRequest(), attrs.getResponse(), policy);
    }
}
