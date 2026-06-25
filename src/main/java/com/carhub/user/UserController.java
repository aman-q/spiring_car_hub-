package com.carhub.user;

import com.carhub.booking.BookingService;
import com.carhub.booking.dto.ProfileBookingsResponse;
import com.carhub.common.message.MessageKeys;
import com.carhub.common.response.ApiResponse;
import com.carhub.common.response.ResponseFactory;
import com.carhub.ratelimit.RateLimit;
import com.carhub.user.dto.AuthResponse;
import com.carhub.user.dto.ForgotPasswordRequest;
import com.carhub.user.dto.LoginRequest;
import com.carhub.user.dto.LogoutRequest;
import com.carhub.user.dto.RefreshTokenRequest;
import com.carhub.user.dto.RegisterRequest;
import com.carhub.user.dto.ResendOtpRequest;
import com.carhub.user.dto.ResetPasswordRequest;
import com.carhub.user.dto.TokenResponse;
import com.carhub.user.dto.UpdateProfileRequest;
import com.carhub.user.dto.UserResponse;
import com.carhub.user.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "Registration, authentication and profile")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final BookingService bookingService;
    private final ResponseFactory response;

    @PostMapping("/register")
    @RateLimit(policy = "register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return response.created(MessageKeys.USER_REGISTERED, authService.register(request));
    }

    @PostMapping("/login")
    @RateLimit(policy = "login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return response.ok(MessageKeys.LOGIN_SUCCESS, authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return response.ok(MessageKeys.OTP_VERIFIED, authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @RateLimit(policy = "otp")
    public ResponseEntity<ApiResponse<UserResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return response.ok(MessageKeys.OTP_RESENT, authService.resendOtp(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return response.ok(MessageKeys.TOKEN_REFRESHED, authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal String userId) {
        authService.logout(authorizationHeader, request == null ? null : request.refreshToken(), userId);
        return response.ok(MessageKeys.LOGOUT_SUCCESS);
    }

    @PostMapping("/forgot-password")
    @RateLimit(policy = "otp")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return response.ok(MessageKeys.PASSWORD_RESET_OTP_SENT);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return response.ok(MessageKeys.PASSWORD_RESET);
    }

    @GetMapping("/profile/me/bookings")
    public ResponseEntity<ApiResponse<ProfileBookingsResponse>> getProfileBookings(
            @AuthenticationPrincipal String userId) {
        ProfileBookingsResponse data = bookingService.getProfileBookings(userId);
        String messageKey = data.upcoming().isEmpty() && data.past().isEmpty()
                ? MessageKeys.NO_BOOKINGS_YET
                : MessageKeys.BOOKINGS_FETCHED;
        return response.ok(messageKey, data);
    }

    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal String userId) {
        return response.ok(MessageKeys.PROFILE_FETCHED, userService.getProfile(userId));
    }

    @PatchMapping("/profile/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return response.ok(MessageKeys.PROFILE_UPDATED, userService.updateProfile(userId, request));
    }
}
