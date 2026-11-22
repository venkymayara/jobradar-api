package com.jobradar.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import com.jobradar.auth.api.AuthSession;
import com.jobradar.auth.api.LoginRequest;
import com.jobradar.auth.api.RegisterRequest;
import com.jobradar.auth.api.UserResponse;
import com.jobradar.auth.role.AppRoleEntity;
import com.jobradar.auth.role.AppRoleRepository;
import com.jobradar.auth.token.RefreshTokenEntity;
import com.jobradar.auth.token.RefreshTokenRepository;
import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final String INVALID_CREDENTIALS = "Email or password is incorrect";
    private static final String INVALID_REFRESH_TOKEN = "Refresh token is invalid or expired";

    private final UserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Duration refreshTokenTtl;

    public AuthService(UserRepository userRepository, AppRoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService, Clock clock, SecureRandom secureRandom,
            @Value("${app.security.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        AppRoleEntity userRole = roleRepository.findById(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("The default user role is not configured"));
        UserEntity user = new UserEntity(request.fullName().strip(), email,
                passwordEncoder.encode(request.password()));
        user.getRoles().add(userRole);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public IssuedAuthSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findWithRolesByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return issueSession(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedAuthSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }

        Instant now = clock.instant();
        RefreshTokenEntity existing = refreshTokenRepository.findForUpdateByHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN));

        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveFamily(existing.getTokenFamilyId(), now);
            throw new UnauthorizedException("Refresh token reuse detected; please sign in again");
        }
        Instant expiresAt = existing.getExpiresAt();
        // Older sliding sessions must use their first sign-in under the fixed lifetime.
        if (expiresAt.isAfter(existing.getCreatedAt().plus(refreshTokenTtl))) {
            expiresAt = refreshTokenRepository
                    .findFamilyStartedAt(existing.getUser().getId(), existing.getTokenFamilyId())
                    .plus(refreshTokenTtl);
        }
        if (!expiresAt.isAfter(now)) {
            refreshTokenRepository.revokeActiveFamily(existing.getTokenFamilyId(), now);
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
        }

        String replacementValue = newRefreshTokenValue();
        RefreshTokenEntity replacement = new RefreshTokenEntity(existing.getUser(), hashToken(replacementValue),
                existing.getTokenFamilyId(), now, expiresAt);
        replacement = refreshTokenRepository.saveAndFlush(replacement);
        existing.revoke(now, replacement.getId());
        refreshTokenRepository.save(existing);
        return issuedSession(existing.getUser(), replacementValue, now, expiresAt);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findForUpdateByHash(hashToken(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(clock.instant(), null);
                refreshTokenRepository.save(token);
            }
        });
    }

    private IssuedAuthSession issueSession(UserEntity user, UUID familyId) {
        String refreshToken = newRefreshTokenValue();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(refreshTokenTtl);
        refreshTokenRepository.save(new RefreshTokenEntity(user, hashToken(refreshToken),
                familyId, now, expiresAt));
        return issuedSession(user, refreshToken, now, expiresAt);
    }

    private IssuedAuthSession issuedSession(UserEntity user, String refreshToken, Instant now, Instant expiresAt) {
        Duration remaining = Duration.between(now, expiresAt);
        long accessSeconds = Math.min(jwtTokenService.accessTokenExpiresInSeconds(), remaining.toSeconds());
        AuthSession session = AuthSession.bearer(jwtTokenService.createAccessToken(user, now, now.plusSeconds(accessSeconds)),
                accessSeconds, remaining.toSeconds(), UserResponse.from(user));
        return new IssuedAuthSession(session, refreshToken, remaining);
    }

    private String newRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    static String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
