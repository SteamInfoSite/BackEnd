package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameIsFreeSearchService {
    GameListResponse getGamesByPrice(
            boolean isFree,
            int page,
            int pageSize
    );
}
