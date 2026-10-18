package com.steaminfosite.security;

import com.steaminfosite.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = {JwtConfig.class, JwtAccessTokenService.class})
@TestPropertySource(properties = {
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.issuer=steaminfosite",
        "app.jwt.access-token-minutes=15"
})
class JwtAccessTokenServiceTest {

    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private JwtAccessTokenService tokenService;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JwtDecoder decoder;

    @Test
    void issuedTokenHasExpectedClaims() {
        Jwt jwt = decoder.decode(tokenService.issueAccessToken(STEAM_ID));

        assertThat(jwt.getSubject()).isEqualTo(STEAM_ID);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("steaminfosite");
        assertThat(jwt.getExpiresAt())
                .isEqualTo(jwt.getIssuedAt().plus(Duration.ofMinutes(15)));
    }

    @Test
    void rejectsTamperedSignature() {
        String[] parts = tokenService.issueAccessToken(STEAM_ID).split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = String.join(".", parts);

        assertThatThrownBy(() -> decoder.decode(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        String expired = encode(
                "steaminfosite",
                now.minus(Duration.ofMinutes(30)),
                now.minus(Duration.ofMinutes(15))
        );

        assertThatThrownBy(() -> decoder.decode(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsDifferentIssuer() {
        Instant now = Instant.now();
        String otherIssuer = encode(
                "other-service",
                now,
                now.plus(Duration.ofMinutes(15))
        );

        assertThatThrownBy(() -> decoder.decode(otherIssuer))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsBlankSteamId() {
        assertThatThrownBy(() -> tokenService.issueAccessToken(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String encode(String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(STEAM_ID)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}