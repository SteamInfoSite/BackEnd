package com.steaminfosite.repository;

import com.steaminfosite.entity.user.RefreshTokenEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.security.RefreshTokenCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {
    @Autowired
    private SiteUserRepository userRepository;

    @Autowired
    private RefreshTokenRepository tokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesHashAndFindsTokenWithUser() {
        SiteUserEntity user = userRepository.save(
                new SiteUserEntity("76561198000000000")
        );

        String rawToken = "test-refresh-token";
        String tokenHash = new RefreshTokenCodec().hash(rawToken);

        tokenRepository.saveAndFlush(new RefreshTokenEntity(
                user,
                tokenHash,
                Instant.now().plus(Duration.ofDays(7))
        ));
        entityManager.clear();

        RefreshTokenEntity saved = tokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow();

        assertThat(saved.getTokenHash()).isEqualTo(tokenHash);
        assertThat(saved.getUser().getSteamId())
                .isEqualTo("76561198000000000");
        assertThat(saved.isUsableAt(Instant.now())).isTrue();

        assertThat(tokenRepository.findByTokenHashForUpdate(rawToken))
                .isEmpty();
    }

    @Test
    void revokesOnlyTheSpecifiedUsersActiveTokens() {
        SiteUserEntity firstUser = userRepository.save(
                new SiteUserEntity("76561198000000001")
        );
        SiteUserEntity otherUser = userRepository.save(
                new SiteUserEntity("76561198000000002")
        );

        Instant now = Instant.parse("2026-10-16T00:00:00Z");
        Instant expiresAt = now.plus(Duration.ofDays(7));

        RefreshTokenEntity first = tokenRepository.saveAndFlush(
                new RefreshTokenEntity(firstUser, "a".repeat(64), expiresAt)
        );
        RefreshTokenEntity second = tokenRepository.saveAndFlush(
                new RefreshTokenEntity(firstUser, "b".repeat(64), expiresAt)
        );
        RefreshTokenEntity other = tokenRepository.saveAndFlush(
                new RefreshTokenEntity(otherUser, "c".repeat(64), expiresAt)
        );

        int revokedCount = tokenRepository.revokeAllActiveForUser(firstUser, now);
        entityManager.clear();

        assertThat(revokedCount).isEqualTo(2);
        assertThat(tokenRepository.findById(first.getId()).orElseThrow()
                .getRevokedAt()).isEqualTo(now);
        assertThat(tokenRepository.findById(second.getId()).orElseThrow()
                .getRevokedAt()).isEqualTo(now);
        assertThat(tokenRepository.findById(other.getId()).orElseThrow()
                .getRevokedAt()).isNull();
    }
}