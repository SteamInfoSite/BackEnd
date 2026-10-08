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
public class GameIsFreeSearchServiceImpl implements GameIsFreeSearchService {

    private final GameRepository gameRepository;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getGamesByPrice(
            boolean isFree,
            int page,
            int pageSize
    ) {
        //잘못된 페이지 조건이나 지나치게 큰 요청을 거절
        if (page < 1 || pageSize < 1 || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지는 1 이상, 페이지 크기는 1~100으로 지정해 주세요"
            );
        }

        //API의 1페이지를 Spring의 0페이지로 변환
        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by("appId").ascending()
        );

        //이미 출시된 게임 중 무료 또는 유료 게임 조회
        Page<GameInfoEntity> games =
                gameRepository.findByTypeAndIsFreeAndComingSoon(
                        "game",
                        isFree,
                        false,
                        pageable
                );

        //DB 엔티티를 목록 응답 DTO로 변환
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