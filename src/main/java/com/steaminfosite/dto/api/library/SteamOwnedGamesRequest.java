package com.steaminfosite.dto.api.library;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SteamOwnedGamesRequest(
        @JsonProperty("steamid")
        String steamId,

        @JsonProperty("include_appinfo")
        boolean includeAppInfo,

        @JsonProperty("include_played_free_games")
        boolean includePlayedFreeGames
) {
}