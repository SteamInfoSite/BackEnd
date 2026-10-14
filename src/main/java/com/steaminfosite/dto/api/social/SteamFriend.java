package com.steaminfosite.dto.api.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamFriend {
    @JsonProperty("steamid")
    private String steamId;

    private String relationship;

    @JsonProperty("friend_since")
    private Long friendSince;
}