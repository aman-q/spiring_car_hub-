package com.carhub.user;

import com.carhub.user.dto.UpdateProfileRequest;
import com.carhub.user.dto.UserResponse;

/** Profile read/update operations for the authenticated user. */
public interface UserService {

    UserResponse getProfile(String userId);

    UserResponse updateProfile(String userId, UpdateProfileRequest request);
}
