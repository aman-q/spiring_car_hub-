package com.carhub.common.security;

import com.carhub.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "unit-test-secret-unit-test-secret-0123456789",
            Duration.ofMinutes(15),
            Duration.ofDays(7)));

    @Test
    void generatesAndParsesAccessToken() {
        JwtService.IssuedToken issued = jwtService.generateAccessToken("user-123");

        assertNotNull(issued.token());
        assertNotNull(issued.jti());

        Claims claims = jwtService.parse(issued.token());
        assertEquals("user-123", claims.getSubject());
        assertEquals(issued.jti(), claims.getId());
    }
}
