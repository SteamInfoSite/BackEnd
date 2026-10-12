package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameHotSearchService {
    GameListResponse getHotGames(
            String platform,
            int page,
            int pageSize
    );
}