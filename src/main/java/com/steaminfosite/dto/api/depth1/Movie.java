package com.steaminfosite.dto.api.depth1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie {
    private Long id;
    private String name;
    private String thumbnail;

    @JsonProperty("hls_h264")
    private String hlsH264;

    private Boolean highlight;
}
