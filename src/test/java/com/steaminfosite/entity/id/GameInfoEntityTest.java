package com.steaminfosite.entity.id;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameInfoEntityTest {

    @Test
    void sameGenresInDifferentOrderKeepExistingObjects() {
        GameGenreEntity action =
                new GameGenreEntity("1", "Action");
        GameGenreEntity adventure =
                new GameGenreEntity("25", "Adventure");

        GameInfoEntity game = GameInfoEntity.builder()
                .genres(List.of(action, adventure))
                .build();

        updateCollections(
                game,
                List.of(),
                List.of(
                        new GameGenreEntity("25", "Adventure"),
                        new GameGenreEntity("1", "Action")
                ),
                List.of()
        );
        //순서만 다르면 기존 객체와 기존 순서를 유지해야 한다
        assertEquals(2, game.getGenres().size());
        assertSame(action, game.getGenres().get(0));
        assertSame(adventure, game.getGenres().get(1));
    }

    @Test
    void changedMovieUrlIsApplied() {
        GameMovieEntity original = new GameMovieEntity(
                10L, "Trailer", "thumbnail.jpg",
                "old.m3u8", true
        );

        GameInfoEntity game = GameInfoEntity.builder()
                .movie(List.of(original))
                .build();

        List<GameMovieEntity> originalCollection = game.getMovie();

        GameMovieEntity changed = new GameMovieEntity(
                10L, "Trailer", "thumbnail.jpg",
                "new.m3u8", true
        );

        updateCollections(
                game,
                List.of(),
                List.of(),
                List.of(changed)
        );
        //컬렉션 자체는 유지하면서 변경된 내용을 반영해야 한다
        assertSame(originalCollection, game.getMovie());
        assertEquals(List.of(changed), game.getMovie());
        assertEquals("new.m3u8", game.getMovie().get(0).getHlsH264());
    }

    @Test
    void changedDuplicateCountsAreApplied() {
        GameInfoEntity game = GameInfoEntity.builder()
                .developers(List.of("A", "A", "B"))
                .build();

        updateCollections(
                game,
                List.of("A", "B", "B"),
                List.of(),
                List.of()
        );
        //종류와 전체 개수가 같아도 항목별 개수가 다르면 변경이다
        assertEquals(
                List.of("A", "B", "B"),
                game.getDevelopers()
        );
    }
    //테스트할 목록 외의 값은 기존 값을 그대로 전달한다
    private void updateCollections(
            GameInfoEntity game,
            List<String> developers,
            List<GameGenreEntity> genres,
            List<GameMovieEntity> movies
    ) {
        game.updateFromSteam(
                game.getGameName(),
                game.getType(),
                game.getRequiredAge(),
                game.getIsFree(),
                game.getShortDescription(),
                game.getHeaderImage(),
                game.getCapsuleImage(),
                game.getComingSoon(),
                game.getRelDate(),
                game.getSupportedLanguages(),
                developers,
                game.getPublishers(),
                genres,
                game.getCategories(),
                movies,
                game.getRecommendationsTotal(),
                game.getWebsite(),
                game.getWindows(),
                game.getMac(),
                game.getLinux()
        );
    }
}