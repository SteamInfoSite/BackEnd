package com.steaminfosite.dto.api.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamPlayerSummariesResponse {
    private SteamPlayerSummariesData response;
}