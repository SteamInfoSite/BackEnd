package com.steaminfosite.dto.api.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamPlayerSummary {

    @JsonProperty("steamid")
    private String steamId;

    @JsonProperty("personaname")
    private String personaName;

    @JsonProperty("profileurl")
    private String profileUrl;

    @JsonProperty("avatarfull")
    private String avatarFull;

    @JsonProperty("personastate")
    private Integer personaState;

    @JsonProperty("communityvisibilitystate")
    private Integer communityVisibilityState;

    @JsonProperty("lastlogoff")
    private Long lastLogoff;

    @JsonProperty("gameid")
    private String gameId;

    @JsonProperty("gameextrainfo")
    private String gameExtraInfo;
}