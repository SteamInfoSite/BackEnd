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
public class GameGenreSearchServiceImpl implements GameGenreSearchService {

    private final GameRepository gameRepository;
    private final GameListResponseMapper gameListResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getGamesByGenre(
            String genre,
            String platform,
            String sort,
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
        String genreName = genre == null || genre.isBlank()
                ? "all"
                : genre.trim().toLowerCase(Locale.ROOT);

        String platformName = platform == null || platform.isBlank()
                ? "all"
                : platform.trim().toLowerCase(Locale.ROOT);

        String sortName = sort == null || sort.isBlank()
                ? "app_id_asc"
                : sort.trim().toLowerCase(Locale.ROOT);
        //지원하는 플랫폼 선택값인지 확인
        if (!List.of("all", "windows", "mac", "linux")
                .contains(platformName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "플랫폼은 all, windows, mac, linux 중에서 선택해 주세요"
            );
        }
        //같은 값인 게임들은 appId 순으로 정렬
        Sort gameSort = switch (sortName) {
            case "app_id_asc" ->
                    Sort.by("appId").ascending();

            case "title_asc" ->
                    Sort.by("gameName").ascending()
                            .and(Sort.by("appId").ascending());

            case "recommendations_desc" ->
                    JpaSort.unsafe(
                            Sort.Direction.DESC,
                            "COALESCE(game.recommendationsTotal, 0)"
                    ).and(Sort.by("appId").ascending());

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다"
            );
        };
        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                gameSort
        );
        Page<GameInfoEntity> games =
                gameRepository.findReleasedGamesByFilters(
                        null,
                        genreName,
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