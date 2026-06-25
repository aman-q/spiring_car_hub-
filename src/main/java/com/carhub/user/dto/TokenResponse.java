package com.carhub.user.dto;

/** Token-refresh payload: rotated access + refresh tokens. */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
