package com.steaminfosite.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.steaminfosite.dto.api.depth1.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private String website;
    @JsonProperty("supported_languages")
    private String supportedLanguages;
    private List<String> developers;
    private List<String> publishers;
    private List<SteamGenre> genres;
    private List<SteamCategory> categories;
    private List<Movie> movies;
    private Recommendations recommendations;
    private Platforms platforms;

}
