package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameDiscountSearchService {
    GameListResponse getDiscountGames(
            String platform,
            String sort,
            int page,
            int pageSize
    );
}