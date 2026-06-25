package com.carhub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotNull Integer otp,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String newPassword
) {
}
