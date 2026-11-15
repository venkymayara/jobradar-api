package com.jobradar.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {
    @Test
    void decoderRejectsExpiredAccessTokens() {
        byte[] secret = "unit-test-secret-that-is-long-enough-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var key = new SecretKeySpec(secret, "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("jobradar-api"));
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("jobradar-api").subject("user@example.com")
                        .issuedAt(now.minusSeconds(180)).expiresAt(now.minusSeconds(120))
                        .claims(claims -> claims.putAll(Map.of("roles", java.util.List.of("ROLE_USER"))))
                        .build())).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtValidationException.class);
    }
}
