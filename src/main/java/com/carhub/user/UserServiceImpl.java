package com.carhub.user;

import com.carhub.common.exception.ApiException;
import com.carhub.common.exception.ErrorCode;
import com.carhub.user.dto.UpdateProfileRequest;
import com.carhub.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        boolean changed = false;

        if (StringUtils.hasText(request.fname())) {
            user.setFname(request.fname());
            changed = true;
        }
        if (StringUtils.hasText(request.lname())) {
            user.setLname(request.lname());
            changed = true;
        }
        if (request.phonenumber() != null) {
            if (userRepository.existsByPhonenumberAndIdNot(request.phonenumber(), userId)) {
                throw new ApiException(ErrorCode.PHONE_ALREADY_IN_USE);
            }
            user.setPhonenumber(request.phonenumber());
            changed = true;
        }

        if (!changed) {
            throw new ApiException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        return userMapper.toResponse(userRepository.save(user));
    }
}
