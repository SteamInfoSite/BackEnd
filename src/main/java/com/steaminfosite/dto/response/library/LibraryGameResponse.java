package com.steaminfosite.dto.response.library;

public record LibraryGameResponse(
        Long appId,
        String gameName,
        String headerImage,
        String iconHash,
        Long playtimeForever,
        Long playtimeTwoWeeks,
        Long lastPlayed,
        boolean detailAvailable
) {
}