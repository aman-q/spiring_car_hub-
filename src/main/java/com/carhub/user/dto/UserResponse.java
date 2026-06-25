package com.carhub.user.dto;

import java.time.Instant;

/**
 * Safe projection of a {@link com.carhub.user.User} returned to clients —
 * no password, OTP or token fields.
 */
public record UserResponse(
        String id,
        String fname,
        String lname,
        Long phonenumber,
        String email,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
}
