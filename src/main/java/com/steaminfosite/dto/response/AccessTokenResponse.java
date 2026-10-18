package com.steaminfosite.dto.response;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        String steamId
) {}