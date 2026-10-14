package com.steaminfosite.repository;

import com.steaminfosite.dto.response.GameReleaseDateTarget;
import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<GameInfoEntity, Long> {
    Optional<GameInfoEntity> findByAppId(Long appId);

    //여러appId에 해당하는 기존상세정보를 한번의 IN쿼리로 조회
    List<GameInfoEntity> findAllByAppIdIn(Collection<Long> appIds);

    //게임 종류 무료 여부, 출시 예정 여부로 페이지 조회
    Page<GameInfoEntity> findByTypeAndIsFreeAndComingSoon(
            String type,
            Boolean isFree,
            Boolean comingSoon,
            Pageable pageable
    );

    //게임 상세에 저장된 장르명을 중복 없이 조회
    @Query("""
        SELECT DISTINCT genre.description
        FROM GameInfoEntity game
        JOIN game.genres genre
        WHERE game.type = 'game'
          AND genre.description IS NOT NULL
          AND trim(genre.description) <> ''
        ORDER BY genre.description
        """)
    List<String> findGameGenreNames();

    //'전체 장르'를 선택했을 때 출시된 게임 조회
    Page<GameInfoEntity> findByTypeAndComingSoon(
            String type,
            Boolean comingSoon,
            Pageable pageable
    );

    //출시된 게임을 무료 여부, 장르, 지원 플랫폼 조건으로 조회
    @Query(
            value = """
                SELECT game
                FROM GameInfoEntity game
                WHERE game.type = 'game'
                  AND game.comingSoon = false
                  AND (:isFree IS NULL OR game.isFree = :isFree)
                  AND (
                      :genreName = 'all'
                      OR EXISTS (
                          SELECT 1
                          FROM GameInfoEntity genreGame
                          JOIN genreGame.genres genre
                          WHERE genreGame.id = game.id
                            AND lower(trim(genre.description))
                                = lower(:genreName)
                      )
                  )
                  AND (
                      :platform = 'all'
                      OR (:platform = 'windows' AND game.windows = true)
                      OR (:platform = 'mac' AND game.mac = true)
                      OR (:platform = 'linux' AND game.linux = true)
                  )
                """,
            countQuery = """
                SELECT COUNT(game.id)
                FROM GameInfoEntity game
                WHERE game.type = 'game'
                  AND game.comingSoon = false
                  AND (:isFree IS NULL OR game.isFree = :isFree)
                  AND (
                      :genreName = 'all'
                      OR EXISTS (
                          SELECT 1
                          FROM GameInfoEntity genreGame
                          JOIN genreGame.genres genre
                          WHERE genreGame.id = game.id
                            AND lower(trim(genre.description))
                                = lower(:genreName)
                      )
                  )
                  AND (
                      :platform = 'all'
                      OR (:platform = 'windows' AND game.windows = true)
                      OR (:platform = 'mac' AND game.mac = true)
                      OR (:platform = 'linux' AND game.linux = true)
                  )
                """
    )
    Page<GameInfoEntity> findReleasedGamesByFilters(
            @Param("isFree") Boolean isFree,
            @Param("genreName") String genreName,
            @Param("platform") String platform,
            Pageable pageable
    );

    //가격 기본 수집 대상: 출시된 유료 게임을 스팀 추천 수 순으로 조회
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    WHERE game.type = 'game'
      AND game.isFree = false
      AND game.comingSoon = false
    ORDER BY COALESCE(game.recommendationsTotal, 0) DESC,
             game.appId ASC
    """)
    List<Long> findPriceCollectionTargetAppIds(Pageable pageable);

    //최초 가격 수집 대상: 아직 정상 확인 기록이 없는 출시된 유료 게임
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    WHERE game.type = 'game'
      AND game.isFree = false
      AND game.comingSoon = false
      AND game.appId > :afterAppId
      AND NOT EXISTS (
          SELECT 1
          FROM GamePriceEntity price
          WHERE price.appId = game.appId
            AND price.priceCheckedAt IS NOT NULL
      )
    ORDER BY game.appId ASC
    """)
    List<Long> findUncollectedPriceAppIdsAfter(
            @Param("afterAppId") Long afterAppId,
            Pageable pageable
    );

    //가격 기록이 없고 재시도 시각이 된 유료 게임을 최근 발견한 순서로 조회
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    JOIN SteamAppEntity app ON app.appId = game.appId
    WHERE game.type = 'game'
      AND game.isFree = false
      AND game.comingSoon = false
      AND (
          app.nextPriceAttemptAt IS NULL
          OR app.nextPriceAttemptAt <= :now
      )
      AND NOT EXISTS (
          SELECT 1
          FROM GamePriceEntity price
          WHERE price.appId = game.appId
      )
    ORDER BY game.createdAt DESC, game.appId ASC
    """)
    List<Long> findMissingPriceAppIdsNewestFirst(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    //저장 당시와 스팀 가격 변경 번호가 다른 게임 조회
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    JOIN SteamAppEntity app ON app.appId = game.appId
    JOIN GamePriceEntity price ON price.appId = game.appId
    WHERE game.type = 'game'
      AND game.isFree = false
      AND game.comingSoon = false
      AND app.priceChangeNumber IS NOT NULL
      AND (
          price.collectedPriceChangeNumber IS NULL
          OR price.collectedPriceChangeNumber <> app.priceChangeNumber
      )
      AND (
          app.nextPriceAttemptAt IS NULL
          OR app.nextPriceAttemptAt <= :now
      )
    ORDER BY game.createdAt DESC, game.appId ASC
    """)
    List<Long> findPriceChangeRefreshAppIds(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    //현재 할인 중이며 마지막 가격 확인이 오래된 게임
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    JOIN SteamAppEntity app ON app.appId = game.appId
    JOIN GamePriceEntity price ON price.appId = game.appId
    WHERE game.type = 'game'
      AND game.isFree = false
      AND game.comingSoon = false
      AND price.priceAvailable = true
      AND price.discountPercent > 0
      AND price.priceCheckedAt <= :refreshBefore
      AND (
          app.nextPriceAttemptAt IS NULL
          OR app.nextPriceAttemptAt <= :now
      )
    ORDER BY price.priceCheckedAt ASC, game.appId ASC
    """)
    List<Long> findStaleDiscountPriceAppIds(
            @Param("now") LocalDateTime now,
            @Param("refreshBefore") LocalDateTime refreshBefore,
            Pageable pageable
    );

    //저장된 가격을 기준으로 할인 게임 조회
    @Query(
            value = """
            SELECT game
            FROM GameInfoEntity game
            JOIN GamePriceEntity price
              ON price.appId = game.appId
            WHERE game.type = 'game'
              AND game.isFree = false
              AND game.comingSoon = false
              AND price.countryCode = 'KR'
              AND price.currency = 'KRW'
              AND price.priceAvailable = true
              AND price.discountPercent > 0
              AND (
                  :platform = 'all'
                  OR (:platform = 'windows' AND game.windows = true)
                  OR (:platform = 'mac' AND game.mac = true)
                  OR (:platform = 'linux' AND game.linux = true)
              )
            """,
            countQuery = """
            SELECT COUNT(game.id)
            FROM GameInfoEntity game
            JOIN GamePriceEntity price
              ON price.appId = game.appId
            WHERE game.type = 'game'
              AND game.isFree = false
              AND game.comingSoon = false
              AND price.countryCode = 'KR'
              AND price.currency = 'KRW'
              AND price.priceAvailable = true
              AND price.discountPercent > 0
              AND (
                  :platform = 'all'
                  OR (:platform = 'windows' AND game.windows = true)
                  OR (:platform = 'mac' AND game.mac = true)
                  OR (:platform = 'linux' AND game.linux = true)
              )
            """
    )
    Page<GameInfoEntity> findDiscountGamesByPlatform(
            @Param("platform") String platform,
            Pageable pageable
    );

    //정확한 날짜 여부를 아직 판별하지 않은 게임 조회
    @Query("""
    SELECT new com.steaminfosite.dto.response.GameReleaseDateTarget(
        game.appId,
        game.relDate
    )
    FROM GameInfoEntity game
    WHERE game.type = 'game'
      AND game.appId > :afterAppId
      AND game.releaseDateExact IS NULL
    ORDER BY game.appId ASC
    """)
    List<GameReleaseDateTarget> findReleaseDateTargetsAfter(
            @Param("afterAppId") Long afterAppId,
            Pageable pageable
    );

    //출시일 원문이 바뀌지 않았고 아직 판별 전이면 두 값을 함께 저장
    @Modifying
    @Query("""
    UPDATE GameInfoEntity game
    SET game.releaseSortDate = :releaseSortDate,
        game.releaseDateExact = :releaseDateExact
    WHERE game.appId = :appId
      AND game.releaseDateExact IS NULL
      AND (
          game.relDate = :originalRelDate
          OR (
              game.relDate IS NULL
              AND :originalRelDate IS NULL
          )
      )
    """)
    int updateReleaseDateInfoIfMissing(
            @Param("appId") Long appId,
            @Param("originalRelDate") String originalRelDate,
            @Param("releaseSortDate") LocalDate releaseSortDate,
            @Param("releaseDateExact") boolean releaseDateExact
    );

    //지정 기간에 출시된 게임을 플랫폼 조건과 함께 조회
    @Query(
            value = """
            SELECT game
            FROM GameInfoEntity game
            WHERE game.type = 'game'
              AND game.comingSoon = false
              AND game.releaseDateExact = true
              AND game.releaseSortDate BETWEEN :startDate AND :endDate
              AND (
                  :platform = 'all'
                  OR (:platform = 'windows' AND game.windows = true)
                  OR (:platform = 'mac' AND game.mac = true)
                  OR (:platform = 'linux' AND game.linux = true)
              )
            """,
            countQuery = """
            SELECT COUNT(game.id)
            FROM GameInfoEntity game
            WHERE game.type = 'game'
              AND game.comingSoon = false
              AND game.releaseDateExact = true
              AND game.releaseSortDate BETWEEN :startDate AND :endDate
              AND (
                  :platform = 'all'
                  OR (:platform = 'windows' AND game.windows = true)
                  OR (:platform = 'mac' AND game.mac = true)
                  OR (:platform = 'linux' AND game.linux = true)
              )
            """
    )
    Page<GameInfoEntity> findNewGamesByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("platform") String platform,
            Pageable pageable
    );

    //출시 예정으로 저장됐지만 예정 날짜가 오늘까지인 게임 조회
    @Query("""
    SELECT game.appId
    FROM GameInfoEntity game
    WHERE game.type = 'game'
      AND game.comingSoon = true
      AND game.releaseSortDate IS NOT NULL
      AND game.releaseSortDate <= :today
      AND game.appId > :afterAppId
    ORDER BY game.appId ASC
    """)
    List<Long> findReleaseCheckTargetAppIdsAfter(
            @Param("today") LocalDate today,
            @Param("afterAppId") Long afterAppId,
            Pageable pageable
    );

    //출시 예정일 상태를 먼저 구분하고 그룹 안의 정렬은 Pageable로 적용
    @Query(
            value = """
        SELECT game
        FROM GameInfoEntity game
        WHERE game.type = 'game'
          AND game.comingSoon = true
          AND (
              :platform = 'all'
              OR (:platform = 'windows' AND game.windows = true)
              OR (:platform = 'mac' AND game.mac = true)
              OR (:platform = 'linux' AND game.linux = true)
          )
        ORDER BY
            CASE
                WHEN game.releaseSortDate IS NULL THEN 2
                WHEN game.releaseSortDate < :today THEN 1
                ELSE 0
            END ASC
        """,
            countQuery = """
        SELECT COUNT(game.id)
        FROM GameInfoEntity game
        WHERE game.type = 'game'
          AND game.comingSoon = true
          AND (
              :platform = 'all'
              OR (:platform = 'windows' AND game.windows = true)
              OR (:platform = 'mac' AND game.mac = true)
              OR (:platform = 'linux' AND game.linux = true)
          )
        """
    )
    Page<GameInfoEntity> findUpcomingGamesByPlatform(
            @Param("platform") String platform,
            @Param("today") LocalDate today,
            Pageable pageable
    );
}