package com.steaminfosite.mapper;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.depth1.Platforms;
import com.steaminfosite.dto.api.depth1.Recommendations;
import com.steaminfosite.dto.api.depth1.ReleaseDate;
import com.steaminfosite.entity.id.GameCategoryEntity;
import com.steaminfosite.entity.id.GameGenreEntity;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.entity.id.GameMovieEntity;
import com.steaminfosite.parser.SteamReleaseDateParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameInfoMapper {
    private final SteamReleaseDateParser steamReleaseDateParser;
    //DB접근없이 SteamDTO를 기존 엔티티에 반영하거나 신규 엔티티로 생성
    public GameInfoEntity mapToEntity(
            SteamApiData gameData,
            Optional<GameInfoEntity> existingGame
    ) {
        ReleaseDate releaseDate = gameData.getReleaseDate();
        Boolean comingSoon = releaseDate != null ? releaseDate.getComingSoon() : null;
        String relDate = releaseDate != null ? releaseDate.getRelDate() : null;
        Recommendations recommendations = gameData.getRecommendations();
        Long recommendationsTotal = recommendations != null ? recommendations.getTotal() : null;
        Platforms platforms = gameData.getPlatforms();
        Boolean windows = platforms != null ? platforms.getWindows() : null;
        Boolean mac = platforms != null ? platforms.getMac() : null;
        Boolean linux = platforms != null ? platforms.getLinux() : null;
        List<GameGenreEntity> genres = gameData.getGenres() ==
                null ? List.of() : gameData.getGenres().stream()
                .map(genre -> new GameGenreEntity(
                        genre.getId(),
                        genre.getDescription()
                ))
                .toList();

        List<GameCategoryEntity> categories = gameData.getCategories() ==
                null ? List.of() : gameData.getCategories().stream()
                .map(category -> new GameCategoryEntity(
                        category.getId(),
                        category.getDescription()
                ))
                .toList();

        List<GameMovieEntity> movies = gameData.getMovies() ==
                null ? List.of() : gameData.getMovies().stream()
                .map(movie -> new GameMovieEntity(
                        movie.getId(),
                        movie.getName(),
                        movie.getThumbnail(),
                        movie.getHlsH264(),
                        movie.getHighlight()
                ))
                .toList();

        GameInfoEntity gameInfoEntity =
                existingGame.map(savedGame -> {
                            savedGame.updateFromSteam(
                                    gameData.getName(),
                                    gameData.getType(),
                                    gameData.getRequiredAge(),
                                    gameData.getIsFree(),
                                    gameData.getShortDescription(),
                                    gameData.getHeaderImage(),
                                    gameData.getCapsuleImage(),
                                    comingSoon,
                                    relDate,
                                    gameData.getSupportedLanguages(),
                                    gameData.getDevelopers(),
                                    gameData.getPublishers(),
                                    genres,
                                    categories,
                                    movies,
                                    recommendationsTotal,
                                    gameData.getWebsite(),
                                    windows,
                                    mac,
                                    linux
                            );
                            return savedGame;
                        })
                        .orElseGet(() -> GameInfoEntity.builder()
                                .appId(gameData.getSteamAppId())
                                .gameName(gameData.getName())
                                .type(gameData.getType())
                                .requiredAge(gameData.getRequiredAge())
                                .isFree(gameData.getIsFree())
                                .shortDescription(gameData.getShortDescription())
                                .headerImage(gameData.getHeaderImage())
                                .capsuleImage(gameData.getCapsuleImage())
                                .website(gameData.getWebsite())
                                .comingSoon(comingSoon)
                                .relDate(relDate)
                                .supportedLanguages(gameData.getSupportedLanguages())
                                .developers(gameData.getDevelopers())
                                .publishers(gameData.getPublishers())
                                .genres(genres)
                                .categories(categories)
                                .movie(movies)
                                .recommendationsTotal(recommendationsTotal)
                                .windows(windows)
                                .mac(mac)
                                .linux(linux)
                                .build());

        gameInfoEntity.updateReleaseSortDate(
                steamReleaseDateParser.parseForSort(relDate)
        );
        gameInfoEntity.updateReleaseDateExact(
                steamReleaseDateParser.parseExactDate(relDate) != null
        );

        return gameInfoEntity;
    }
}
