package com.carhub.user;

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

/**
 * Account document. Sensitive fields (password, otp, refresh token) never leave the
 * service layer — controllers only ever see {@code UserResponse}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fname;
    private String lname;

    @Indexed(unique = true)
    private Long phonenumber;

    @Indexed(unique = true)
    private String email;

    private String password;

    private Integer otp;
    private Instant otpExpiry;

    @Builder.Default
    private boolean emailVerified = false;

    private String refreshToken;
    private Instant refreshTokenExpiry;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
