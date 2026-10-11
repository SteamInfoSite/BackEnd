package com.steaminfosite.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GamePriceResponse {
    private String countryCode;
    private String currency;

    private Long initialAmount;
    private Long finalAmount;

    private Integer discountPercent;

    private String initialFormatted;
    private String finalFormatted;

    private boolean priceAvailable;

    private LocalDateTime priceCheckedAt;
}