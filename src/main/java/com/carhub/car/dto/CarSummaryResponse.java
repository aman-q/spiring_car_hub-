package com.carhub.car.dto;

import com.carhub.car.DriveType;

import java.util.List;

/** Compact car projection for the paginated listing, with live availability. */
public record CarSummaryResponse(
        String id,
        String title,
        String description,
        Double price,
        List<String> images,
        String company,
        Integer yearOfManufacture,
        DriveType driveType,
        boolean currentlyAvailable
) {
}
