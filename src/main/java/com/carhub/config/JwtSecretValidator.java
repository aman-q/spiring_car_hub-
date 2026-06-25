package com.carhub.config;

import com.carhub.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Fails the application fast if the JWT secret is unsafe: shorter than the 32 bytes
 * HS256 needs, or still the committed dev placeholder while running under a non-dev
 * profile (e.g. {@code prod}). Prevents shipping with a guessable signing key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecretValidator {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String DEV_SECRET_MARKER = "dev-only-insecure";

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "carhub.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256.");
        }

        boolean nonDevProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production")
                        || p.equalsIgnoreCase("staging"));
        if (nonDevProfile && secret.contains(DEV_SECRET_MARKER)) {
            throw new IllegalStateException(
                    "The default dev JWT secret must not be used outside local development. "
                            + "Set a strong JWT_SECRET.");
        }

        if (secret.contains(DEV_SECRET_MARKER)) {
            log.warn("Using the default dev JWT secret — override JWT_SECRET before deploying.");
        }
    }
}
