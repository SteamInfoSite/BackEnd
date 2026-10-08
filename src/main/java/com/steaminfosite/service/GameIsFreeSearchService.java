package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameIsFreeSearchService {
    GameListResponse getGamesByPrice(
            boolean isFree,
            String genre,
            String platform,
            String sort,
            int page,
            int pageSize
    );
}
