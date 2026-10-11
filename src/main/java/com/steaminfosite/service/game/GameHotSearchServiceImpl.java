package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.mapper.GameListResponseMapper;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GameHotSearchServiceImpl implements GameHotSearchService {

    private final GameRepository gameRepository;
    private final GameListResponseMapper gameListResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getHotGames(
            String platform,
            int page,
            int pageSize
    ) {
        if (page < 1 || pageSize < 1 || pageSize > 100
                || ((long) page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지는 1 이상, 페이지 크기는 1~100으로 지정해 주세요"
            );
        }

        String platformName = platform == null || platform.isBlank()
                ? "all"
                : platform.trim().toLowerCase(Locale.ROOT);

        if (!List.of("all", "windows", "mac", "linux")
                .contains(platformName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "플랫폼은 all, windows, mac, linux 중에서 선택해 주세요"
            );
        }
        //추천 수가 같으면 appId 순으로 표시
        Sort gameSort = JpaSort.unsafe(
                Sort.Direction.DESC,
                "COALESCE(game.recommendationsTotal, 0)"
        ).and(Sort.by("appId").ascending());

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                gameSort
        );
        //무료 유료 모두 포함하고 장르 제한 없이 조회
        Page<GameInfoEntity> games =
                gameRepository.findReleasedGamesByFilters(
                        null,
                        "all",
                        platformName,
                        pageable
                );

        List<GameListResponse.Item> items =
                games.getContent().stream()
                        .map(gameListResponseMapper::toItem)
                        .toList();

        return new GameListResponse(
                items,
                games.getTotalElements(),
                page,
                pageSize
        );
    }
}