package com.steaminfosite.dto.api.price;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamPriceOverview {

    private String currency;

    @JsonProperty("initial")
    private Long initialAmount;

    @JsonProperty("final")
    private Long finalAmount;

    @JsonProperty("discount_percent")
    private Integer discountPercent;

    @JsonProperty("initial_formatted")
    private String initialFormatted;

    @JsonProperty("final_formatted")
    private String finalFormatted;
}