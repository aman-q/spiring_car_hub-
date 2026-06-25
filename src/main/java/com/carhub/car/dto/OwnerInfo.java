package com.carhub.car.dto;

/** Public subset of the car owner's profile. */
public record OwnerInfo(
        String id,
        String fname,
        String lname,
        String email
) {
}
