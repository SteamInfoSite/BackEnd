package com.steaminfosite.repository;

import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SteamAppRepository extends JpaRepository<SteamAppEntity, Long> {
    Optional<SteamAppEntity> findByAppId(Long appId);
    List<SteamAppEntity> findAllByAppIdIn(Collection<Long> appIds);

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

    //아직 상세정보를 수집하지 않은 앱을 최근 발견한 순서로 조회
    @Query("""
    SELECT app
    FROM SteamAppEntity app
    WHERE app.detailStatus = :notFetchedStatus
    ORDER BY app.createdAt DESC, app.appId ASC
    """)
    List<SteamAppEntity> findNotFetchedTargets(
            @Param("notFetchedStatus") GameDetailStatus notFetchedStatus,
            Pageable pageable
    );
}
