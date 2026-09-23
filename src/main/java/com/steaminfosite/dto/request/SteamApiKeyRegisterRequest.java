package com.steaminfosite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SteamApiKeyRegisterRequest(

        @NotBlank(message = "스팀API키가 필요합니다")
        @Pattern(
                regexp = "^[0-9A-Fa-f]{32}$",
                message = "Steam API 키는 32자리 16진수 형식이어야 합니다"
        )
        String steamApiKey
) {

}