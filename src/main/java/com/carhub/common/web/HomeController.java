package com.carhub.common.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Landing endpoint for the bare domain root. Without it, {@code GET /} falls through to
 * the static-resource handler, throws {@code NoResourceFoundException}, and surfaces as a
 * 500 — making a healthy deployment look broken. Returns a small service descriptor (and
 * doubles as a cheap liveness target for platform health checks pointed at {@code /}).
 */
@Tag(name = "Meta", description = "Service metadata")
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "carhub",
                "status", "UP",
                "api", "/api/v1",
                "docs", "/swagger-ui.html",
                "health", "/actuator/health");
    }
}
