package com.carhub.car.dto;

import com.carhub.car.DriveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Multipart form fields for creating a car (images are bound separately). */
@Getter
@Setter
public class AddCarRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Integer yearOfManufacture;

    @NotBlank
    private String company;

    @NotNull
    private DriveType driveType;

    @NotNull
    @Positive
    private Double price;
}
