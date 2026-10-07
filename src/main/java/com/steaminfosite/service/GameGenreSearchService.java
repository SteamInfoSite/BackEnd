package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameGenreSearchService {

    GameListResponse getGamesByGenre(
            String genre,
            int page,
            int pageSize
    );
}