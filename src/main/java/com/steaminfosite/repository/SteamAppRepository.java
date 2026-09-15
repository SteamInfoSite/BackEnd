package com.steaminfosite.repository;

import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SteamAppRepository extends JpaRepository<SteamAppEntity, Long> {
    Optional<SteamAppEntity> findByAppId(Long appId);

    //일반 수집대상과 재시도 조건을 만족한 API오류 대상을 함께 조회
    @Query("""
        SELECT app
        FROM SteamAppEntity app
        WHERE app.detailStatus IN :normalStatuses
           OR (
               app.detailStatus = :apiErrorStatus
               AND COALESCE(app.apiErrorRetryCount, 0) <= :maxApiErrorRetryCount
               AND (
                   app.nextApiErrorRetryAt IS NULL
                   OR app.nextApiErrorRetryAt <= :now
               )
           )
        ORDER BY app.appId ASC
        """)
    List<SteamAppEntity> findCollectTargets(
            @Param("normalStatuses") List<GameDetailStatus> normalStatuses,
            @Param("apiErrorStatus") GameDetailStatus apiErrorStatus,
            @Param("maxApiErrorRetryCount") int maxApiErrorRetryCount,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
