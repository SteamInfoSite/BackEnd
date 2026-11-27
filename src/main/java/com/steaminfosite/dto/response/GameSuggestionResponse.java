package com.steaminfosite.dto.response;

public record GameSuggestionResponse(
        Long appId,
        String gameName,
        String image
) {
}