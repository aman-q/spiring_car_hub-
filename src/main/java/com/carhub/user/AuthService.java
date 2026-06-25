package com.carhub.user;

import com.carhub.user.dto.AuthResponse;
import com.carhub.user.dto.ForgotPasswordRequest;
import com.carhub.user.dto.LoginRequest;
import com.carhub.user.dto.RefreshTokenRequest;
import com.carhub.user.dto.RegisterRequest;
import com.carhub.user.dto.ResendOtpRequest;
import com.carhub.user.dto.ResetPasswordRequest;
import com.carhub.user.dto.TokenResponse;
import com.carhub.user.dto.UserResponse;
import com.carhub.user.dto.VerifyOtpRequest;

/**
 * Authentication + account lifecycle: registration, OTP verification, login,
 * refresh-token rotation, logout, and password reset.
 */
public interface AuthService {

    UserResponse register(RegisterRequest request);

    UserResponse verifyOtp(VerifyOtpRequest request);

    UserResponse resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(String authorizationHeader, String refreshToken, String userId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
