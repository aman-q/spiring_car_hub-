package com.carhub.car.dto;

import java.util.List;

public record PagedCarsResponse(
        List<CarSummaryResponse> cars,
        int totalPages,
        int currentPage,
        long totalCars
) {
}
