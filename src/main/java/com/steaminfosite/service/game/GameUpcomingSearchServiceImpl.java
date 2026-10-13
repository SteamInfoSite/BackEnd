package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.dto.response.GamePriceResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.mapper.GameListResponseMapper;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.service.price.GamePriceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameUpcomingSearchServiceImpl
        implements GameUpcomingSearchService {
    private final GameRepository gameRepository;
    private final GameListResponseMapper gameListResponseMapper;
    private final GamePriceSearchService gamePriceSearchService;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse getUpcomingGames(
            String platform,
            String sort,
            int page,
            int pageSize
    ) {
        if (page < 1 || pageSize < 1 || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "페이지는 1 이상, 페이지 크기는 1~100으로 지정해 주세요"
            );
        }
        String platformName = platform == null || platform.isBlank()
                ? "all"
                : platform.trim().toLowerCase(Locale.ROOT);

        String sortName = sort == null || sort.isBlank()
                ? "released_asc"
                : sort.trim().toLowerCase(Locale.ROOT);

        if (!List.of("all", "windows", "mac", "linux")
                .contains(platformName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "플랫폼은 all, windows, mac, linux 중에서 선택해 주세요"
            );
        }

        Sort releaseDateSort = switch (sortName) {
            case "released_asc" ->
                    Sort.by("releaseSortDate").ascending();

            case "released_desc" ->
                    Sort.by("releaseSortDate").descending();

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다"
            );
        };
        //Repository에서 예정일 상태를 먼저 정렬한 뒤,
        //각 그룹 안에서 출시일과 appId 순서를 적용
        Sort gameSort = releaseDateSort
                .and(Sort.by("appId").ascending());

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Seoul")
        );

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                gameSort
        );

        Page<GameInfoEntity> games =
                gameRepository.findUpcomingGamesByPlatform(
                        platformName,
                        today,
                        pageable
                );
        //현재 페이지에 포함된 게임의 저장된 가격 조회
        List<Long> appIds = games.getContent().stream()
                .map(GameInfoEntity::getAppId)
                .toList();

        Map<Long, GamePriceResponse> prices = gamePriceSearchService.getPricesByAppIds(appIds);

        List<GameListResponse.Item> items =
                games.getContent().stream()
                        .map(game -> gameListResponseMapper.toItem(
                                game,
                                prices.get(game.getAppId()),
                                today
                        ))
                        .toList();

        return new GameListResponse(
                items,
                games.getTotalElements(),
                page,
                pageSize
        );
    }
}