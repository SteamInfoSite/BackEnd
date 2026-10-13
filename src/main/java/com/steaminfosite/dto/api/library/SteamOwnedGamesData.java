package com.steaminfosite.dto.api.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamOwnedGamesData {
    @JsonProperty("game_count")
    private Integer gameCount;

    private List<SteamOwnedGame> games;
}