package com.steaminfosite.service.user;

import com.steaminfosite.entity.user.RefreshTokenEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.RefreshTokenRepository;
import com.steaminfosite.security.JwtAccessTokenService;
import com.steaminfosite.security.RefreshTokenCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final RefreshTokenCodec codec;
    private final JwtAccessTokenService accessTokenService;
    private final long refreshTokenDays;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            RefreshTokenCodec codec,
            JwtAccessTokenService accessTokenService,
            @Value("${app.auth.refresh-token-days}") long refreshTokenDays
    ) {
        if (refreshTokenDays <= 0) {
            throw new IllegalArgumentException("재발급 토큰 유효기간은 0보다 커야 합니다");
        }
        this.repository = repository;
        this.codec = codec;
        this.accessTokenService = accessTokenService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public IssuedTokens issueForLogin(SiteUserEntity user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("저장된 사용자 정보가 필요합니다");
        }
        return issueTokens(user, Instant.now());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return new RotationResult(RotationStatus.INVALID, null);
        }

        RefreshTokenEntity stored = repository
                .findByTokenHashForUpdate(codec.hash(rawToken))
                .orElse(null);

        if (stored == null) {
            return new RotationResult(RotationStatus.INVALID, null);
        }

        Instant now = Instant.now();

        if (stored.getRevokedAt() != null) {
            repository.revokeAllActiveForUser(stored.getUser(), now);
            return new RotationResult(RotationStatus.REUSED, null);
        }

        if (!stored.isUsableAt(now)) {
            return new RotationResult(RotationStatus.INVALID, null);
        }

        stored.revoke(now);
        return new RotationResult(
                RotationStatus.SUCCESS,
                issueTokens(stored.getUser(), now)
        );
    }

    @Transactional
    public void revokeOnLogout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        repository.findByTokenHashForUpdate(codec.hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private IssuedTokens issueTokens(SiteUserEntity user, Instant now) {
        String refreshToken = codec.generate();
        repository.save(new RefreshTokenEntity(
                user,
                codec.hash(refreshToken),
                now.plus(Duration.ofDays(refreshTokenDays))
        ));

        String accessToken = accessTokenService.issueAccessToken(user.getSteamId());
        return new IssuedTokens(accessToken, refreshToken, user.getSteamId());
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            String steamId
    ) {}

    public enum RotationStatus {
        SUCCESS, INVALID, REUSED
    }

    public record RotationResult(
            RotationStatus status,
            IssuedTokens tokens
    ) {}
}