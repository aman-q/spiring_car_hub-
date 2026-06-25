package com.carhub.user;

import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.common.message.MessageKeys;
import com.carhub.common.message.MessageService;
import com.carhub.common.security.JwtService;
import com.carhub.common.security.TokenBlacklist;
import com.carhub.common.security.Tokens;
import com.carhub.config.properties.JwtProperties;
import com.carhub.email.EmailService;
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
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int REFRESH_TOKEN_BYTES = 40;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final JwtProperties jwtProperties;
    private final EmailService emailService;
    private final MessageService messageService;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailOrPhonenumber(request.email(), request.phonenumber())) {
            throw new ApiException(ErrorCode.USER_ALREADY_EXISTS);
        }

        int otp = generateOtp();
        User user = User.builder()
                .fname(request.fname())
                .lname(request.lname())
                .email(request.email())
                .phonenumber(request.phonenumber())
                .password(passwordEncoder.encode(request.password()))
                .otp(otp)
                .otpExpiry(Instant.now().plus(OTP_TTL))
                .emailVerified(false)
                .build();
        User saved = userRepository.save(user);
        log.info("User registered: {}", saved.getId());

        emailService.sendOtp(saved.getFname(), saved.getEmail(), String.valueOf(otp),
                messageService.get(MessageKeys.EMAIL_SUBJECT_VERIFY_OTP));

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        if (user.getOtp() == null || user.getOtpExpiry() == null || !user.getOtp().equals(request.otp())) {
            throw new ApiException(ErrorCode.INVALID_OTP);
        }
        if (user.getOtpExpiry().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.OTP_EXPIRED);
        }

        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        int otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(Instant.now().plus(OTP_TTL));
        User saved = userRepository.save(user);

        emailService.sendOtp(saved.getFname(), saved.getEmail(), String.valueOf(otp),
                messageService.get(MessageKeys.EMAIL_SUBJECT_RESEND_OTP));

        return userMapper.toResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Single generic error for unknown email or bad password — avoids user enumeration.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        JwtService.IssuedToken accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefreshToken = issueRefreshToken(user);
        userRepository.save(user);
        log.info("User logged in: {}", user.getId());

        return new AuthResponse(accessToken.token(), rawRefreshToken, userMapper.toResponse(user));
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hashed = Tokens.sha256Hex(request.refreshToken());
        User user = userRepository.findByRefreshTokenAndRefreshTokenExpiryAfter(hashed, Instant.now())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        // Rotate both tokens on every refresh.
        JwtService.IssuedToken accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefreshToken = issueRefreshToken(user);
        userRepository.save(user);

        return new TokenResponse(accessToken.token(), rawRefreshToken);
    }

    @Override
    public void logout(String authorizationHeader, String refreshToken, String userId) {
        blacklistAccessToken(authorizationHeader);
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
        log.info("User logged out: {}", userId);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        int otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(Instant.now().plus(OTP_TTL));
        User saved = userRepository.save(user);

        emailService.sendOtp(saved.getFname(), saved.getEmail(), String.valueOf(otp),
                messageService.get(MessageKeys.EMAIL_SUBJECT_RESET_PASSWORD));
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getOtp() == null || user.getOtpExpiry() == null || !user.getOtp().equals(request.otp())) {
            throw new ApiException(ErrorCode.INVALID_OTP);
        }
        if (user.getOtpExpiry().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.OTP_EXPIRED);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getId());
    }

    /** Generates a 6-digit OTP (100000–999999). */
    private int generateOtp() {
        return 100_000 + RANDOM.nextInt(900_000);
    }

    /** Issues a new raw refresh token, persisting only its hash + expiry on the user. */
    private String issueRefreshToken(User user) {
        String rawRefreshToken = Tokens.randomHex(REFRESH_TOKEN_BYTES);
        user.setRefreshToken(Tokens.sha256Hex(rawRefreshToken));
        user.setRefreshTokenExpiry(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        return rawRefreshToken;
    }

    /** Blacklists the bearer access token for its remaining lifetime, best-effort. */
    private void blacklistAccessToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return;
        }
        String token = authorizationHeader.startsWith(BEARER_PREFIX)
                ? authorizationHeader.substring(BEARER_PREFIX.length())
                : authorizationHeader;
        try {
            Claims claims = jwtService.parse(token);
            tokenBlacklist.blacklist(claims.getId(),
                    Duration.between(Instant.now(), claims.getExpiration().toInstant()));
        } catch (Exception e) {
            log.debug("Could not blacklist access token on logout: {}", e.getMessage());
        }
    }
}
