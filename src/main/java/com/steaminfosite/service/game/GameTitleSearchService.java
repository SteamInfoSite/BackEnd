package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.dto.response.GameSuggestionResponse;

import java.util.List;

public interface GameTitleSearchService {
    GameListResponse search(String q, String sort, int page, int pageSize);
    List<GameSuggestionResponse> suggest(String q);
}