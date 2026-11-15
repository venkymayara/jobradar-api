package com.jobradar.auth.api;

public record AuthSession(String accessToken, String tokenType, long expiresIn, UserResponse user) {
    public static AuthSession bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthSession(accessToken, "Bearer", expiresIn, user);
    }
}
