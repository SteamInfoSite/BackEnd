package com.steaminfosite.dto.api.depth1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDate{
    @JsonProperty("coming_soon")
    private Boolean comingSoon;
    @JsonProperty("date")
    private String relDate;
}
