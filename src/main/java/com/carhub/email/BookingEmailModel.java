package com.carhub.email;

/**
 * Flat, presentation-ready view model for booking emails. Dates are pre-formatted
 * so templates contain no logic, and the email module stays decoupled from the
 * booking/car/user domain documents.
 */
public record BookingEmailModel(
        String recipientName,
        String recipientEmail,
        String carTitle,
        String carCompany,
        String bookingRef,
        String startDate,
        String endDate,
        String pickupLocation,
        String dropoffLocation,
        double totalPrice,
        long days,
        String status
) {
}
