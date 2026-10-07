package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameInfoResponse.Platforms;
import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.entity.id.GameGenreEntity;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameGenreSearchServiceImpl implements GameGenreSearchService {

    private final GameRepository gameRepository;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getGamesByGenre(
            String genre,
            int page,
            int pageSize
    ) {
        //페이지 요청 검증
        if (page < 1 || pageSize < 1 || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지는 1 이상, 페이지 크기는 1~100으로 지정해 주세요"
            );
        }
        //장르 선택값이 없으면 전체 조회
        String genreName = genre == null || genre.isBlank()
                ? "all"
                : genre.trim();

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by("appId").ascending()
        );

        Page<GameInfoEntity> games;

        if ("all".equalsIgnoreCase(genreName)) {
            games = gameRepository.findByTypeAndComingSoon(
                    "game",
                    false,
                    pageable
            );
        } else {
            games = gameRepository.findReleasedGamesByGenreName(
                    genreName,
                    pageable
            );
        }
        //조회한 게임을 기존 목록 응답 DTO로 변환
        List<GameListResponse.Item> items =
                games.getContent().stream()
                        .map(this::toListItem)
                        .toList();

        return new GameListResponse(
                items,
                games.getTotalElements(),
                page,
                pageSize
        );
    }

    private GameListResponse.Item toListItem(GameInfoEntity game) {
        return GameListResponse.Item.builder()
                .appId(game.getAppId())
                .gameName(game.getGameName())
                .headerImage(game.getHeaderImage())
                .capsuleImage(game.getCapsuleImage())
                .isFree(game.getIsFree())
                .comingSoon(game.getComingSoon())
                .relDate(game.getRelDate())
                .genres(
                        game.getGenres().stream()
                                .map(GameGenreEntity::getDescription)
                                .toList()
                )
                .platforms(
                        Platforms.builder()
                                .windows(game.getWindows())
                                .mac(game.getMac())
                                .linux(game.getLinux())
                                .build()
                )
                .recommendationsTotal(game.getRecommendationsTotal())
                .updatedAt(game.getUpdatedAt())
                .build();
    }
}