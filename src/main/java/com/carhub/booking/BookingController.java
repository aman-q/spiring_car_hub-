package com.carhub.booking;

import com.carhub.booking.dto.BookingResponse;
import com.carhub.booking.dto.CreateBookingRequest;
import com.carhub.common.message.MessageKeys;
import com.carhub.common.response.ApiResponse;
import com.carhub.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Bookings", description = "Booking lifecycle")
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ResponseFactory response;

    @PostMapping("/new-booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateBookingRequest request) {
        return response.created(MessageKeys.BOOKING_CREATED, bookingService.createBooking(userId, request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) BookingStatus status) {
        return response.ok(MessageKeys.BOOKINGS_FETCHED, bookingService.getUserBookings(userId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return response.ok(MessageKeys.BOOKING_DETAIL_FETCHED, bookingService.getBookingById(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return response.ok(MessageKeys.BOOKING_CANCELLED, bookingService.cancelBooking(userId, id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return response.ok(MessageKeys.BOOKING_COMPLETED, bookingService.completeBooking(userId, id));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return response.ok(MessageKeys.BOOKING_CONFIRMED, bookingService.confirmBooking(userId, id));
    }
}
