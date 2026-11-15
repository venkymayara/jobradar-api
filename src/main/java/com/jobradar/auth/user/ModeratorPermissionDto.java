package com.jobradar.auth.user;

public record ModeratorPermissionDto(Integer userId, String email, boolean moderator, String updatedBy) {}
