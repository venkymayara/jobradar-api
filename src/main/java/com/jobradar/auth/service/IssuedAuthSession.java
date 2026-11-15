package com.jobradar.auth.service;

import java.time.Duration;

import com.jobradar.auth.api.AuthSession;

public record IssuedAuthSession(AuthSession session, String refreshToken, Duration refreshTokenTtl) {
}
