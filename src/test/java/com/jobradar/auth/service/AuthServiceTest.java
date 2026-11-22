package com.jobradar.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.jobradar.auth.api.LoginRequest;
import com.jobradar.auth.api.RegisterRequest;
import com.jobradar.auth.role.AppRoleEntity;
import com.jobradar.auth.role.AppRoleRepository;
import com.jobradar.auth.token.RefreshTokenEntity;
import com.jobradar.auth.token.RefreshTokenRepository;
import com.jobradar.auth.user.UserEntity;
import com.jobradar.auth.user.UserRepository;
import com.jobradar.common.exception.ConflictException;
import com.jobradar.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");
    private static final String ENCODED_PASSWORD = "$2a$12$encodedPasswordHash";
    private static final String ACCESS_TOKEN = "signed-access-token";
    private static final Duration REFRESH_TTL = Duration.ofHours(8);

    @Mock private UserRepository userRepository;
    @Mock private AppRoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;

    private AuthService service;
    private AppRoleEntity userRole;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        java.security.SecureRandom secureRandom = new java.security.SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                java.util.Arrays.fill(bytes, (byte) 7);
            }
        };
        service = new AuthService(userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenService, clock, secureRandom, REFRESH_TTL);
        userRole = new AppRoleEntity("ROLE_USER");
        lenient().when(jwtTokenService.createAccessToken(any(UserEntity.class), any(Instant.class), any(Instant.class)))
                .thenReturn(ACCESS_TOKEN);
        lenient().when(jwtTokenService.accessTokenExpiresInSeconds()).thenReturn(900L);
    }

    @Test
    void registersWithHashedPasswordAndUserRoleOnly() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(roleRepository.findById("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("GoodPassword123!")).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42);
            return saved;
        });

        var response = service.register(new RegisterRequest("  Ada Lovelace  ", " PERSON@EXAMPLE.COM ",
                "GoodPassword123!"));

        assertThat(response.userId()).isEqualTo(42);
        assertThat(response.displayName()).isEqualTo("Ada Lovelace");
        assertThat(response.email()).isEqualTo("person@example.com");
        assertThat(response.roles()).containsExactly("ROLE_USER");
        verify(roleRepository).findById("ROLE_USER");
        verify(passwordEncoder).encode("GoodPassword123!");
    }

    @Test
    void duplicateRegistrationReturnsConflict() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Ada", "person@example.com", "GoodPassword123!")))
                .isInstanceOf(ConflictException.class);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void invalidCredentialsUseSameUnauthorizedResponseForUnknownEmail() {
        when(userRepository.findWithRolesByEmail("person@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("person@example.com", "WrongPassword1!")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email or password is incorrect");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void validLoginCreatesAccessAndHashedRefreshTokens() {
        UserEntity user = user("person@example.com");
        when(userRepository.findWithRolesByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("GoodPassword123!", ENCODED_PASSWORD)).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var issued = service.login(new LoginRequest("PERSON@example.com", "GoodPassword123!"));

        assertThat(issued.session().accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(issued.session().tokenType()).isEqualTo("Bearer");
        assertThat(issued.session().sessionExpiresIn()).isEqualTo(REFRESH_TTL.toSeconds());
        assertThat(issued.session().user().roles()).containsExactly("ROLE_USER");
        assertThat(issued.refreshToken()).isNotBlank();
        ArgumentCaptor<RefreshTokenEntity> tokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(issued.refreshToken());
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
    }

    @Test
    void wrongPasswordIsRejected() {
        UserEntity user = user("person@example.com");
        when(userRepository.findWithRolesByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123!", ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("person@example.com", "WrongPassword123!")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email or password is incorrect");
    }

    @Test
    void refreshRotatesTokenAndRevokesThePreviousValue() {
        UserEntity user = user("person@example.com");
        UUID familyId = UUID.randomUUID();
        RefreshTokenEntity oldToken = new RefreshTokenEntity(user, AuthService.hashToken("old-refresh"),
                familyId, NOW.minusSeconds(10), NOW.plusSeconds(30));
        when(refreshTokenRepository.findForUpdateByHash(AuthService.hashToken("old-refresh")))
                .thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.saveAndFlush(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var rotated = service.refresh("old-refresh");

        assertThat(rotated.refreshToken()).isNotEqualTo("old-refresh");
        assertThat(oldToken.getRevokedAt()).isEqualTo(NOW);
        verify(refreshTokenRepository).save(oldToken);
        ArgumentCaptor<RefreshTokenEntity> tokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).saveAndFlush(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(rotated.refreshToken());
        assertThat(tokenCaptor.getValue().getTokenFamilyId()).isEqualTo(familyId);
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(oldToken.getExpiresAt());
        assertThat(rotated.refreshTokenTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(rotated.session().expiresIn()).isEqualTo(30);
        assertThat(rotated.session().sessionExpiresIn()).isEqualTo(30);
        verify(jwtTokenService).createAccessToken(user, NOW, NOW.plusSeconds(30));
    }

    @Test
    void legacySlidingTokenExpiresFromTheOriginalSignIn() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenEntity token = new RefreshTokenEntity(user("person@example.com"),
                AuthService.hashToken("legacy"), familyId, NOW.minusSeconds(60), NOW.plus(Duration.ofDays(29)));
        when(refreshTokenRepository.findForUpdateByHash(AuthService.hashToken("legacy")))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.findFamilyStartedAt(5, familyId)).thenReturn(NOW.minus(Duration.ofHours(9)));

        assertThatThrownBy(() -> service.refresh("legacy"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token is invalid or expired");
        verify(refreshTokenRepository).revokeActiveFamily(familyId, NOW);
        verify(refreshTokenRepository, never()).saveAndFlush(any(RefreshTokenEntity.class));
    }

    @Test
    void expiredRefreshTokenRevokesItsFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenEntity expired = new RefreshTokenEntity(user("person@example.com"),
                AuthService.hashToken("expired-refresh"), familyId, NOW.minusSeconds(100), NOW);
        when(refreshTokenRepository.findForUpdateByHash(AuthService.hashToken("expired-refresh")))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("expired-refresh"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token is invalid or expired");
        verify(refreshTokenRepository).revokeActiveFamily(eq(familyId), eq(NOW));
    }

    @Test
    void refreshTokenReuseRevokesTheWholeFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshTokenEntity used = new RefreshTokenEntity(user("person@example.com"),
                AuthService.hashToken("reused-refresh"), familyId, NOW.minusSeconds(30), NOW.plusSeconds(30));
        used.revoke(NOW.minusSeconds(5), null);
        when(refreshTokenRepository.findForUpdateByHash(AuthService.hashToken("reused-refresh")))
                .thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.refresh("reused-refresh"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("reuse detected");
        verify(refreshTokenRepository).revokeActiveFamily(eq(familyId), eq(NOW));
    }

    @Test
    void logoutRevokesThePresentedRefreshToken() {
        RefreshTokenEntity token = new RefreshTokenEntity(user("person@example.com"),
                AuthService.hashToken("logout-refresh"), UUID.randomUUID(), NOW, NOW.plus(REFRESH_TTL));
        when(refreshTokenRepository.findForUpdateByHash(AuthService.hashToken("logout-refresh")))
                .thenReturn(Optional.of(token));

        service.logout("logout-refresh");

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
        verify(refreshTokenRepository).save(token);
    }

    private UserEntity user(String email) {
        UserEntity user = new UserEntity("Test Person", email, ENCODED_PASSWORD);
        ReflectionTestUtils.setField(user, "id", 5);
        user.getRoles().add(userRole);
        return user;
    }
}
