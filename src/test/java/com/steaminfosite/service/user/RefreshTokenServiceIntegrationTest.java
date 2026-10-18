package com.steaminfosite.service.user;

import com.steaminfosite.entity.user.RefreshTokenEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.RefreshTokenRepository;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.RefreshTokenCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceIntegrationTest {
    private static final AtomicLong USER_SEQUENCE = new AtomicLong(76561198900000000L);

    @Autowired private RefreshTokenService service;
    @Autowired private RefreshTokenRepository tokenRepository;
    @Autowired private SiteUserRepository userRepository;
    @Autowired private RefreshTokenCodec codec;
    @Autowired private JwtDecoder jwtDecoder;

    @Test
    void loginIssuesAccessTokenAndStoresRefreshHash() {
        SiteUserEntity user = newUser();

        RefreshTokenService.IssuedTokens issued = service.issueForLogin(user);

        assertThat(jwtDecoder.decode(issued.accessToken()).getSubject())
                .isEqualTo(user.getSteamId());
        assertThat(stored(issued.refreshToken()).getTokenHash())
                .isNotEqualTo(issued.refreshToken());
        assertThat(stored(issued.refreshToken())
                .isUsableAt(Instant.now())).isTrue();
    }

    @Test
    void rotationRevokesOldTokenAndIssuesNewTokens() {
        var first = service.issueForLogin(newUser());

        var rotated = service.rotate(first.refreshToken());

        assertThat(rotated.status())
                .isEqualTo(RefreshTokenService.RotationStatus.SUCCESS);
        assertThat(rotated.tokens().refreshToken())
                .isNotEqualTo(first.refreshToken());
        assertThat(stored(first.refreshToken()).getRevokedAt())
                .isNotNull();
        assertThat(stored(rotated.tokens().refreshToken())
                .isUsableAt(Instant.now())).isTrue();
    }

    @Test
    void reusingOldTokenRevokesNewToken() {
        var first = service.issueForLogin(newUser());
        var rotated = service.rotate(first.refreshToken());

        var reused = service.rotate(first.refreshToken());

        assertThat(reused.status())
                .isEqualTo(RefreshTokenService.RotationStatus.REUSED);
        assertThat(reused.tokens()).isNull();
        assertThat(stored(rotated.tokens().refreshToken()).getRevokedAt())
                .isNotNull();
    }

    @Test
    void logoutRevokesRefreshToken() {
        var issued = service.issueForLogin(newUser());

        service.revokeOnLogout(issued.refreshToken());

        assertThat(stored(issued.refreshToken()).getRevokedAt())
                .isNotNull();
    }

    @Test
    void expiredTokenCannotRotate() {
        SiteUserEntity user = newUser();
        String rawToken = codec.generate();
        tokenRepository.saveAndFlush(new RefreshTokenEntity(
                user,
                codec.hash(rawToken),
                Instant.now().minus(Duration.ofHours(1))
        ));

        var result = service.rotate(rawToken);

        assertThat(result.status())
                .isEqualTo(RefreshTokenService.RotationStatus.INVALID);
        assertThat(result.tokens()).isNull();
    }

    private SiteUserEntity newUser() {
        return userRepository.save(
                new SiteUserEntity(
                        Long.toString(USER_SEQUENCE.incrementAndGet())
                )
        );
    }

    private RefreshTokenEntity stored(String rawToken) {
        String hash = codec.hash(rawToken);
        return tokenRepository.findAll().stream()
                .filter(token -> token.getTokenHash().equals(hash))
                .findFirst()
                .orElseThrow();
    }
}