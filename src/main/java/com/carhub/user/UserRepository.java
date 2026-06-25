package com.carhub.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmailOrPhonenumber(String email, Long phonenumber);

    boolean existsByPhonenumberAndIdNot(Long phonenumber, String id);

    Optional<User> findByRefreshTokenAndRefreshTokenExpiryAfter(String refreshToken, Instant now);
}
