package com.steaminfosite.dto.api.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamOwnedGamesResponse {
    private SteamOwnedGamesData response;
}