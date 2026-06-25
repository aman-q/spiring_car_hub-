package com.carhub.common.security;

import com.carhub.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies short-lived access tokens (HS256). The token's {@code sub}
 * carries the user id and its {@code jti} is used for logout blacklisting.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public IssuedToken generateAccessToken(String userId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);
        String token = Jwts.builder()
                .subject(userId)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, expiresAt);
    }

    /** Verifies signature + expiry and returns the claims, or throws {@link io.jsonwebtoken.JwtException}. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public record IssuedToken(String token, String jti, Instant expiresAt) {
    }
}
