package com.patternrun.account;

import com.patternrun.account.dto.UserResponse;

/** Entity to response, matching the static-mapper convention of the content layer. */
public final class AccountMapper {

    private AccountMapper() {
    }

    public static UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.isAnonymous(),
                user.getTimezone(),
                user.getCreatedAt());
    }
}
