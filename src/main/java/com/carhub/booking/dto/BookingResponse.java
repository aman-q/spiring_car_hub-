package com.carhub.booking.dto;

import com.carhub.booking.BookingStatus;
import com.carhub.booking.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Booking projection. The nested {@code car}/{@code user} summaries are populated only
 * where the service resolves them (lists / detail) and omitted otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(
        String id,
        String userId,
        String carId,
        LocalDate startDate,
        LocalDate endDate,
        String pickupLocation,
        String dropoffLocation,
        BookingStatus status,
        PaymentStatus paymentStatus,
        Double price,
        Map<String, Object> extras,
        Instant createdAt,
        Instant updatedAt,
        BookingCarInfo car,
        BookingUserInfo user
) {
}
