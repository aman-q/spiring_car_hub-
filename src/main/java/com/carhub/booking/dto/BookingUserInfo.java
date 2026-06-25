package com.carhub.booking.dto;

/** User summary embedded in a booking detail response. */
public record BookingUserInfo(
        String id,
        String fname,
        String lname,
        String email
) {
}
