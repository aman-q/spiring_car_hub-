package com.carhub.car.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityResponse(
        boolean currentlyBooked,
        LocalDate nextAvailableFrom,
        List<BookedPeriod> bookedPeriods
) {
}
