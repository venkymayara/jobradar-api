package com.jobradar.auth.api;

public record AuthSession(String accessToken, String tokenType, long expiresIn, long sessionExpiresIn, UserResponse user) {
    public static AuthSession bearer(String accessToken, long expiresIn, long sessionExpiresIn, UserResponse user) {
        return new AuthSession(accessToken, "Bearer", expiresIn, sessionExpiresIn, user);
    }
}
