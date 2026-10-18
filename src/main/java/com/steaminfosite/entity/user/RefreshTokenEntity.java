package com.steaminfosite.entity.user;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "auth_refresh_token",
        indexes = @Index(
                name = "idx_auth_refresh_token_user_id",
                columnList = "user_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenEntity extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SiteUserEntity user;

    @Column(name = "token_hash", nullable = false, unique = true,
            updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public RefreshTokenEntity(
            SiteUserEntity user,
            String tokenHash,
            Instant expiresAt
    ) {
        if (user == null || tokenHash == null || tokenHash.isBlank()
                || expiresAt == null) {
            throw new IllegalArgumentException("재발급 토큰 정보가 필요합니다");
        }
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}