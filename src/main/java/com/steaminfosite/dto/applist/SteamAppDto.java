package com.steaminfosite.dto.applist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamAppDto {
    @JsonProperty("appid")
    private Long appId;

    private String name;

    @JsonProperty("last_modified")
    private Long lastModified;

    @JsonProperty("price_change_number")
    private Long priceChangeNumber;
}
