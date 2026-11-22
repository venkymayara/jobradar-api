package com.jobradar.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.jobradar.auth.user.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration accessTokenTtl;
    private final String issuer;

    public JwtTokenService(JwtEncoder jwtEncoder, Clock clock,
            @Value("${app.security.jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${app.security.jwt.issuer}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
        this.issuer = issuer;
    }

    public String createAccessToken(UserEntity user) {
        Instant now = clock.instant();
        return createAccessToken(user, now, now.plus(accessTokenTtl));
    }

    public String createAccessToken(UserEntity user, Instant issuedAt, Instant expiresAt) {
        List<String> roles = user.getRoles().stream().map(role -> role.getRole()).sorted().toList();
        String fullName = user.getFullName() == null ? "" : user.getFullName();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("fullName", fullName)
                .claim("roles", roles)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
