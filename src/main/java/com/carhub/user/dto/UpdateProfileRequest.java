package com.carhub.user.dto;

import jakarta.validation.constraints.Positive;

/** All fields optional; at least one must be present (enforced in the service). */
public record UpdateProfileRequest(
        String fname,
        String lname,
        @Positive Long phonenumber
) {
}
