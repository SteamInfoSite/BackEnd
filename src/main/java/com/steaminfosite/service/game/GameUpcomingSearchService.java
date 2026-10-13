package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameUpcomingSearchService {
    GameListResponse getUpcomingGames(
            String platform,
            String sort,
            int page,
            int pageSize
    );
}