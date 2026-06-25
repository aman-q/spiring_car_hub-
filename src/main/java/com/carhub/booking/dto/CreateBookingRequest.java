package com.carhub.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record CreateBookingRequest(
        @NotBlank String carId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String pickupLocation,
        String dropoffLocation,
        Map<String, Object> extras
) {
}
