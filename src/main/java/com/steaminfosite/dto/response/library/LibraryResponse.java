package com.steaminfosite.dto.response.library;

import java.time.Instant;
import java.util.List;

public record LibraryResponse(
        List<LibraryGameResponse> games,
        List<LibraryGameResponse> unclassifiedGames,
        Instant checkedAt
) {
}