package com.carhub.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Rental booking. {@code user} and {@code car} hold the related document ids.
 * {@code deleted} drives soft-deletion; all read queries filter it out.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    @Indexed
    private String user;

    @Indexed
    private String car;

    private LocalDate startDate;
    private LocalDate endDate;

    private String pickupLocation;
    private String dropoffLocation;

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private Double price;

    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();

    @Builder.Default
    private boolean deleted = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
