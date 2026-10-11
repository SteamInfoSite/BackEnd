package com.steaminfosite.repository;

import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}