package com.steaminfosite.dto.api.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamOwnedGame {
    @JsonProperty("appid")
    private Long appId;

    private String name;

    @JsonProperty("img_icon_url")
    private String iconHash;

    @JsonProperty("playtime_forever")
    private Long playtimeForever;

    @JsonProperty("playtime_2weeks")
    private Long playtimeTwoWeeks;

    @JsonProperty("rtime_last_played")
    private Long lastPlayed;
}