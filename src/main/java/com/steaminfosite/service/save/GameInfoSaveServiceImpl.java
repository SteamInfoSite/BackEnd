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

import java.util.*;

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
        //DB에 같은 appId의 게임이 있는지 확인
        Optional<GameInfoEntity> existingGame = gameRepository.findByAppId(gameData.getSteamAppId());
        //Steam데이터와 기존게임을 다른 메서드에 전달, 기존게임이 있으면 내용을 수정, 없으면 새엔티티 반환
        GameInfoEntity gameInfoEntity = buildOrUpdateGameInfoEntity(gameData, existingGame);
        return GameInfoResponse.from(   //반환받은 엔티티를 DB에 저장
                gameRepository.save(gameInfoEntity)
        );
    }
    //DB접근없이 SteamDTO를 기존 엔티티에 반영하거나 신규 엔티티로 생성
    private static GameInfoEntity buildOrUpdateGameInfoEntity(
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
        return gameInfoEntity;
    }

    //전달받은 상세정보 목록 전체를 하나의 트랜잭션 안에서 저장
    @Override
    @Transactional
    public int saveOrUpdateGameInfo(List<SteamApiData> gameDataList) {
        if (gameDataList.isEmpty()) {
            return 0;
        }
        //Steam DTO목록에서 조회에 사용할 appId만 추출
        List<Long> appIds = new ArrayList<>();

        for (SteamApiData gameData : gameDataList) {
            appIds.add(gameData.getSteamAppId());
        }

        //기존 상세정보를 한번의 IN쿼리로 조회한뒤 appId기준으로 보관
        Map<Long, GameInfoEntity> existingGamesByAppId = new HashMap<>();

        for (GameInfoEntity existingGame :
                gameRepository.findAllByAppIdIn(appIds)) {
            existingGamesByAppId.put(
                    existingGame.getAppId(),
                    existingGame
            );
        }
        //조회결과를 이용해 DB추가 조회없이 엔티티 목록생성
        List<GameInfoEntity> gameInfoEntities = new ArrayList<>();

        for (SteamApiData gameData : gameDataList) {
            Optional<GameInfoEntity> existingGame =
                    Optional.ofNullable(
                            existingGamesByAppId.get(
                                    gameData.getSteamAppId()
                            )
                    );
            gameInfoEntities.add(
                    buildOrUpdateGameInfoEntity(
                            gameData,
                            existingGame
                    )
            );
        }//변환된 엔티티 목록을 현재 트랜잭션 안에서 한 번에 저장 요청
        gameRepository.saveAll(gameInfoEntities);
        return gameInfoEntities.size();
    }
}
