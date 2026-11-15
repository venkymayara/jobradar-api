package com.jobradar.auth.api;

import java.util.List;

import com.jobradar.auth.user.UserEntity;

public record UserResponse(int userId, String displayName, String email, List<String> roles) {
    public static UserResponse from(UserEntity user) {
        String displayName = user.getFullName() == null ? "" : user.getFullName();
        return new UserResponse(user.getId(), displayName, user.getEmail(),
                user.getRoles().stream().map(role -> role.getRole()).sorted().toList());
    }
}
