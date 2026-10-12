package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameNewSearchService {
    GameListResponse getNewGames(
            String platform,
            String sort,
            int page,
            int pageSize
    );
}