package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.dto.response.GamePriceResponse;
import com.steaminfosite.dto.response.GameSuggestionResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.mapper.GameListResponseMapper;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.service.price.GamePriceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import java.util.Locale;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameTitleSearchServiceImpl implements GameTitleSearchService {

    private final GameRepository gameRepository;
    private final GameListResponseMapper gameListResponseMapper;
    private final GamePriceSearchService gamePriceSearchService;

    @Override
    @Transactional(readOnly = true)
    public GameListResponse search(String q, String sort, int page, int pageSize) {
        String keyword = q == null ? "" : q.trim();

        if (keyword.isEmpty() || keyword.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "검색어는 1~100자로 입력해 주세요"
            );
        }

        if (page < 1 || pageSize < 1 || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "페이지 또는 페이지 크기가 올바르지 않습니다"
            );
        }

        String sortName = sort == null || sort.isBlank()
                ? "recommendations_desc"
                : sort.trim().toLowerCase(Locale.ROOT);

        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<GameInfoEntity> games = switch (sortName) {
            case "recommendations_desc" ->
                    gameRepository.findPopularTitleMatches(keyword, pageable);
            case "released_desc" ->
                    gameRepository.findLatestTitleMatches(keyword, pageable);
            case "title_asc" ->
                    gameRepository.findByTypeAndGameNameContainingIgnoreCase(
                            "game", keyword,
                            PageRequest.of(page - 1, pageSize,
                                    Sort.by("gameName").ascending()
                                            .and(Sort.by("appId").ascending()))
                    );
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 방식입니다"
            );
        };

        List<Long> appIds = games.getContent().stream()
                .map(GameInfoEntity::getAppId)
                .toList();

        Map<Long, GamePriceResponse> prices =
                gamePriceSearchService.getPricesByAppIds(appIds);

        List<GameListResponse.Item> items = games.getContent().stream()
                .map(game -> gameListResponseMapper.toItem(
                        game, prices.get(game.getAppId())
                ))
                .toList();

        return new GameListResponse(
                items, games.getTotalElements(), page, pageSize
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSuggestionResponse> suggest(String q) {
        String keyword = q == null ? "" : q.trim();

        if (keyword.length() < 2) {
            return List.of();
        }
        if (keyword.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "검색어는 100자 이하로 입력해 주세요"
            );
        }

        return gameRepository.findTitleSuggestions(
                keyword, PageRequest.of(0, 6)
        );
    }
}