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

    //선택한 영문 장르명이 붙어 있는 출시된 게임 조회
    @Query(
            value = """
                SELECT DISTINCT game
                FROM GameInfoEntity game
                JOIN game.genres genre
                WHERE game.type = 'game'
                  AND game.comingSoon = false
                  AND lower(trim(genre.description)) = lower(:genreName)
                """,
            countQuery = """
                SELECT COUNT(DISTINCT game.id)
                FROM GameInfoEntity game
                JOIN game.genres genre
                WHERE game.type = 'game'
                  AND game.comingSoon = false
                  AND lower(trim(genre.description)) = lower(:genreName)
                """
    )
    Page<GameInfoEntity> findReleasedGamesByGenreName(
            @Param("genreName") String genreName,
            Pageable pageable
    );
}
