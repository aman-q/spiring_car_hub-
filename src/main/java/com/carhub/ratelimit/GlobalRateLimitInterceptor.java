package com.carhub.ratelimit;

import com.carhub.config.properties.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies the {@code global} rate-limit policy to every request (the equivalent of
 * the Node {@code globalRateLimiter} mounted on the whole app). Runs inside the
 * dispatcher so a violation is rendered by the {@code GlobalExceptionHandler}.
 */
@Component
@RequiredArgsConstructor
public class GlobalRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.enabled() && properties.global() != null) {
            rateLimitService.enforce(request, response, properties.global());
        }
        return true;
    }
}
