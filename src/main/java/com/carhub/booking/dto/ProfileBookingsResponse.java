package com.carhub.booking.dto;

import java.util.List;

public record ProfileBookingsResponse(
        List<BookingResponse> upcoming,
        List<BookingResponse> past
) {
}
