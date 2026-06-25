package com.carhub.car.dto;

import com.carhub.booking.BookingStatus;

import java.time.LocalDate;

public record BookedPeriod(
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status
) {
}
