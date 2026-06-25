package com.carhub.booking.dto;

import java.util.List;

/** Car summary embedded in a booking response. */
public record BookingCarInfo(
        String id,
        String title,
        String company,
        Double price,
        List<String> images,
        String addedBy
) {
}
