package com.jobradar.auth.api;

import com.jobradar.auth.user.UserEntity;

public record UserPreferencesResponse(boolean emailNotificationsEnabled, boolean jobAlertsEnabled) {
    public static UserPreferencesResponse from(UserEntity user) { return new UserPreferencesResponse(user.isEmailNotificationsEnabled(), user.isJobAlertsEnabled()); }
}
