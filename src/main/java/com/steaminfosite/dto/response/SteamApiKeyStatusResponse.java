package com.steaminfosite.dto.response;

import com.steaminfosite.entity.type.SteamApiKeyStatus;

public record SteamApiKeyStatusResponse(
        SteamApiKeyStatus status,
        boolean usable,
        String message
) {
    public static SteamApiKeyStatusResponse from(
            SteamApiKeyStatus status
    ) {
        return switch (status) {
            case VALID -> new SteamApiKeyStatusResponse(
                    status,
                    true,
                    "스팀API키가 등록되었습니다"
            );
            case INVALID -> new SteamApiKeyStatusResponse(
                    status,
                    false,
                    "유효하지 않은 스팀API키입니다"
            );
            case NOT_REGISTERED -> new SteamApiKeyStatusResponse(
                    status,
                    false,
                    "스팀API키가 등록되어 있지 않습니다"
            );
        };
    }
}