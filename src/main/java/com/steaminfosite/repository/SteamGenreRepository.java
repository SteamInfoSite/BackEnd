package com.steaminfosite.repository;

import com.steaminfosite.entity.id.SteamGenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteamGenreRepository
        extends JpaRepository<SteamGenreEntity, String> {
    List<SteamGenreEntity> findAllByOrderByGenreNameAsc();

    //실제 게임에 붙어 있는 장르명만 선택 목록으로 조회
    @Query("""
        SELECT catalog.genreName
        FROM SteamGenreEntity catalog
        WHERE EXISTS (
            SELECT 1
            FROM GameInfoEntity game
            JOIN game.genres genre
            WHERE game.type = 'game'
              AND lower(trim(genre.description))
                  = lower(trim(catalog.genreName))
        )
        ORDER BY catalog.genreName
        """)
    List<String> findGameGenreNames();
}

