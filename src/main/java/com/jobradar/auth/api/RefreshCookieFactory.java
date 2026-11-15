package com.jobradar.auth.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieFactory {
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final String cookieName;
    private final String sameSite;
    private final boolean secure;

    public RefreshCookieFactory(
            @Value("${app.security.refresh-cookie-name}") String cookieName,
            @Value("${app.security.refresh-cookie-same-site:Strict}") String sameSite,
            @Value("${app.security.refresh-cookie-secure:true}") boolean secure) {
        this.cookieName = cookieName;
        this.sameSite = sameSite;
        this.secure = secure;
    }

    public ResponseCookie create(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    public String cookieName() {
        return cookieName;
    }
}
