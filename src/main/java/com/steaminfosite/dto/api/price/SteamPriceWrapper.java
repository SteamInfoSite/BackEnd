package com.steaminfosite.dto.api.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamPriceWrapper {
    private Boolean success;

    @JsonDeserialize(using = SteamPriceDataDeserializer.class)
    private SteamPriceData data;
}