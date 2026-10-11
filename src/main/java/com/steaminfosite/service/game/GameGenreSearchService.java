package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameGenreSearchService {
    GameListResponse getGamesByGenre(
            String genre,
            String platform,
            String sort,
            int page,
            int pageSize
    );
}