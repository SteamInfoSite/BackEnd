package com.steaminfosite.exception;

import lombok.Getter;

@Getter
public class SteamUserApiException extends SteamApiException {
    private final int statusCode;
    private final String retryAfter;

    public SteamUserApiException(
            int statusCode,
            String retryAfter
    ) {
        super(
                "스팀 사용자 API 요청 실패: HTTP " + statusCode,
                null
        );

        this.statusCode = statusCode;
        this.retryAfter = retryAfter;
    }
}