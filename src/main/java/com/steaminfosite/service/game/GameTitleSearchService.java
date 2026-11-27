package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;

public interface GameTitleSearchService {
    GameListResponse search(String q, int page, int pageSize);
}