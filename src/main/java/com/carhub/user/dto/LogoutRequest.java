package com.carhub.user.dto;

/** Refresh token to invalidate on logout. Optional — logout also blacklists the access token. */
public record LogoutRequest(
        String refreshToken
) {
}
