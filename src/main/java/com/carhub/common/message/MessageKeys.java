package com.carhub.common.message;

/**
 * Centralised keys for success messages (error keys live on {@link
 * com.carhub.common.exception.ErrorCode}). Controllers reference these constants
 * so no message key is hardcoded as a magic string at the call site.
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    // Auth
    public static final String USER_REGISTERED = "success.auth.registered";
    public static final String LOGIN_SUCCESS = "success.auth.login";
    public static final String OTP_VERIFIED = "success.auth.otp-verified";
    public static final String OTP_RESENT = "success.auth.otp-resent";
    public static final String TOKEN_REFRESHED = "success.auth.token-refreshed";
    public static final String LOGOUT_SUCCESS = "success.auth.logout";
    public static final String PASSWORD_RESET_OTP_SENT = "success.auth.reset-otp-sent";
    public static final String PASSWORD_RESET = "success.auth.password-reset";

    // Profile
    public static final String PROFILE_FETCHED = "success.profile.fetched";
    public static final String PROFILE_UPDATED = "success.profile.updated";
    public static final String NO_BOOKINGS_YET = "success.profile.no-bookings";

    // Cars
    public static final String CARS_FETCHED = "success.car.fetched";
    public static final String CAR_ADDED = "success.car.added";
    public static final String CAR_UPDATED = "success.car.updated";
    public static final String CAR_DELETED = "success.car.deleted";
    public static final String USER_CARS_FETCHED = "success.car.user-cars";
    public static final String CAR_DETAIL_FETCHED = "success.car.detail";

    // Bookings
    public static final String BOOKING_CREATED = "success.booking.created";
    public static final String BOOKING_CANCELLED = "success.booking.cancelled";
    public static final String BOOKING_COMPLETED = "success.booking.completed";
    public static final String BOOKING_CONFIRMED = "success.booking.confirmed";
    public static final String BOOKINGS_FETCHED = "success.booking.fetched";
    public static final String BOOKING_DETAIL_FETCHED = "success.booking.detail";

    // Email subjects
    public static final String EMAIL_SUBJECT_VERIFY_OTP = "email.subject.verify-otp";
    public static final String EMAIL_SUBJECT_RESEND_OTP = "email.subject.resend-otp";
    public static final String EMAIL_SUBJECT_RESET_PASSWORD = "email.subject.reset-password";
    public static final String EMAIL_SUBJECT_BOOKING_CONFIRMATION = "email.subject.booking-confirmation";
    public static final String EMAIL_SUBJECT_BOOKING_CANCELLATION = "email.subject.booking-cancellation";
    public static final String EMAIL_SUBJECT_BOOKING_COMPLETION = "email.subject.booking-completion";
}
