package com.carhub.car.dto;

import com.carhub.car.DriveType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Full car projection. {@code owner} and {@code currentlyBooked} are only populated
 * in contexts that compute them (detail / user-cars) and omitted otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarResponse(
        String id,
        String addedBy,
        String title,
        String description,
        List<String> images,
        Map<String, Object> tags,
        Integer yearOfManufacture,
        String company,
        DriveType driveType,
        Double price,
        Instant createdAt,
        Instant updatedAt,
        OwnerInfo owner,
        Boolean currentlyBooked
) {
}
