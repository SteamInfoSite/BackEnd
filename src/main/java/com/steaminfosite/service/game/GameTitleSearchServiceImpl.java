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
    public GameListResponse search(String q, int page, int pageSize) {
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

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by("gameName").ascending()
                        .and(Sort.by("appId").ascending())
        );

        Page<GameInfoEntity> games =
                gameRepository.findByTypeAndGameNameContainingIgnoreCase(
                        "game", keyword, pageable
                );

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
}