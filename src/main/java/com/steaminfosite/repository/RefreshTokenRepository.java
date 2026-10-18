package com.steaminfosite.repository;

import com.steaminfosite.entity.user.RefreshTokenEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from RefreshTokenEntity token
            where token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        update RefreshTokenEntity token
        set token.revokedAt = :revokedAt
        where token.user = :user
          and token.revokedAt is null
        """)
    int revokeAllActiveForUser(
            @Param("user") SiteUserEntity user,
            @Param("revokedAt") Instant revokedAt
    );
}