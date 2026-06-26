package com.carhub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Single source of truth that pairs every business error with its HTTP status and
 * its message-bundle key. Call sites throw {@link ApiException} with one of these
 * constants and never hardcode a status code or a message string.
 */
@Getter
public enum ErrorCode {

    // ── Auth & user ───────────────────────────────────────────────────────────
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.user.already-exists"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not-found"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.auth.invalid-credentials"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "error.auth.email-not-verified"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "error.auth.email-already-verified"),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "error.auth.invalid-otp"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "error.auth.otp-expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "error.auth.invalid-refresh-token"),
    REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "error.auth.refresh-token-required"),
    TOKEN_INVALIDATED(HttpStatus.UNAUTHORIZED, "error.auth.token-invalidated"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "error.auth.same-password"),
    PHONE_ALREADY_IN_USE(HttpStatus.CONFLICT, "error.user.phone-in-use"),
    NO_FIELDS_TO_UPDATE(HttpStatus.BAD_REQUEST, "error.common.no-fields-to-update"),

    // ── Cars ──────────────────────────────────────────────────────────────────
    CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "error.car.not-found"),
    INVALID_ID(HttpStatus.BAD_REQUEST, "error.common.invalid-id"),
    AT_LEAST_THREE_IMAGES(HttpStatus.BAD_REQUEST, "error.car.min-images"),
    CAR_NO_PRICING(HttpStatus.INTERNAL_SERVER_ERROR, "error.car.no-pricing"),
    CAR_HAS_ACTIVE_BOOKINGS(HttpStatus.CONFLICT, "error.car.has-active-bookings"),
    UNAUTHORIZED_CAR_ACTION(HttpStatus.FORBIDDEN, "error.car.unauthorized-action"),

    // ── Bookings ──────────────────────────────────────────────────────────────
    BOOKING_IN_PROGRESS(HttpStatus.CONFLICT, "error.booking.in-progress"),
    INVALID_BOOKING_DATES(HttpStatus.BAD_REQUEST, "error.booking.invalid-dates"),
    CAR_ALREADY_BOOKED(HttpStatus.CONFLICT, "error.booking.car-already-booked"),
    INVALID_EXTRA_OPTION(HttpStatus.BAD_REQUEST, "error.booking.invalid-extra"),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "error.booking.not-found"),
    BOOKING_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "error.booking.already-cancelled"),
    BOOKING_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "error.booking.already-completed"),
    CANNOT_CANCEL_STARTED_BOOKING(HttpStatus.BAD_REQUEST, "error.booking.cannot-cancel-started"),
    CANNOT_COMPLETE_FUTURE_BOOKING(HttpStatus.BAD_REQUEST, "error.booking.cannot-complete-future"),
    BOOKING_MUST_BE_CONFIRMED(HttpStatus.BAD_REQUEST, "error.booking.must-be-confirmed"),
    BOOKING_MUST_BE_PENDING(HttpStatus.BAD_REQUEST, "error.booking.must-be-pending"),
    UNAUTHORIZED_BOOKING_ACTION(HttpStatus.FORBIDDEN, "error.booking.unauthorized-action"),

    // ── General / cross-cutting ───────────────────────────────────────────────
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.common.validation"),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.common.route-not-found"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "error.common.rate-limit"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "error.common.unauthenticated"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.common.access-denied"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.common.server-error");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }
}
