package com.carhub.booking;

import com.carhub.booking.dto.BookingResponse;
import com.carhub.booking.dto.CreateBookingRequest;
import com.carhub.booking.dto.ProfileBookingsResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(String userId, CreateBookingRequest request);

    BookingResponse cancelBooking(String userId, String bookingId);

    BookingResponse completeBooking(String userId, String bookingId);

    BookingResponse confirmBooking(String userId, String bookingId);

    List<BookingResponse> getUserBookings(String userId, BookingStatus status);

    BookingResponse getBookingById(String userId, String bookingId);

    ProfileBookingsResponse getProfileBookings(String userId);
}
