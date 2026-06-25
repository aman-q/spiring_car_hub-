package com.carhub.car.dto;

public record CarDetailResponse(
        CarResponse car,
        AvailabilityResponse availability
) {
}
