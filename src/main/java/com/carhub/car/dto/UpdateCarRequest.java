package com.carhub.car.dto;

import com.carhub.car.DriveType;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Multipart form fields for updating a car; all optional, images bound separately. */
@Getter
@Setter
public class UpdateCarRequest {

    private String title;
    private String description;
    private Integer yearOfManufacture;
    private String company;
    private DriveType driveType;

    @Positive
    private Double price;
}
