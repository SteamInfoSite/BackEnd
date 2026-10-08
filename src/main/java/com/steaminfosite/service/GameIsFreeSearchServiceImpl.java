package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameInfoResponse.Platforms;
import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.entity.id.GameGenreEntity;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
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
public class GameIsFreeSearchServiceImpl implements GameIsFreeSearchService {

    private final GameRepository gameRepository;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getGamesByPrice(
            boolean isFree,
            String genre,
            String platform,
            String sort,
            int page,
            int pageSize
    ) {
        // 페이지 요청 검증
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

        if (!List.of("all", "windows", "mac", "linux")
                .contains(platformName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "플랫폼은 all, windows, mac, linux 중에서 선택해 주세요"
            );
        }
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
        //무료 여부와 장르 플랫폼 조건을 함께 적용
        Page<GameInfoEntity> games =
                gameRepository.findReleasedGamesByFilters(
                        isFree,
                        genreName,
                        platformName,
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