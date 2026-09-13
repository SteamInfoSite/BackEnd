package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.depth1.Platforms;
import com.steaminfosite.dto.api.depth1.Recommendations;
import com.steaminfosite.dto.api.depth1.ReleaseDate;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.entity.id.GameCategoryEntity;
import com.steaminfosite.entity.id.GameGenreEntity;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.entity.id.GameMovieEntity;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameInfoSaveServiceImpl implements GameInfoSaveService{
    private final GameRepository gameRepository;

    //상세정보는 조회만 하므로 readOnly 트랜잭션으로 불필요한 변경 감지를 줄임
    @Override
    @Transactional(readOnly = true)
    public Optional<GameInfoResponse> findSavedGameInfo(Long appId) {
        return gameRepository.findByAppId(appId)
                .map(GameInfoResponse::from);
    }

    //Steam 상세 API DTO를 GameInfoEntity로 변환한 뒤 DB에 저장
    @Override
    @Transactional
    public GameInfoResponse saveOrUpdateGameInfo(SteamApiData gameData) {
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
                gameRepository.findByAppId(gameData.getSteamAppId())
                        .map(savedGame -> {
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
                                .supportedLanguages(
                                        gameData.getSupportedLanguages()
                                )
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

        return GameInfoResponse.from(
                gameRepository.save(gameInfoEntity)
        );
    }
}
