package com.carhub.user.dto;

/** Login payload: a fresh access token, a rotating refresh token, and the user profile. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
