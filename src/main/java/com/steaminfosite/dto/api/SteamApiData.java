package com.steaminfosite.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.steaminfosite.dto.api.depth1.ReleaseDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamApiData {
    private String type;
    private String name;
    @JsonProperty("steam_appid")
    private Long steamAppId;
    @JsonProperty("required_age")
    private Long requiredAge;
    @JsonProperty("is_free")
    private Boolean isFree;
    @JsonProperty("short_description")
    private String shortDescription;
    @JsonProperty("header_image")
    private String headerImage;
    @JsonProperty("capsule_image")
    private String capsuleImage;
    @JsonProperty("release_date")
    private ReleaseDate releaseDate;
}
