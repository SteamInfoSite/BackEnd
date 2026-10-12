package com.steaminfosite.mapper;

import com.steaminfosite.dto.response.GamePriceResponse;
import com.steaminfosite.entity.price.GamePriceEntity;
import org.springframework.stereotype.Component;

@Component
public class GamePriceResponseMapper {
    public GamePriceResponse toResponse(GamePriceEntity price) {
        return GamePriceResponse.builder()
                .countryCode(price.getCountryCode())
                .currency(price.getCurrency())
                .initialAmount(price.getInitialAmount())
                .finalAmount(price.getFinalAmount())
                .discountPercent(price.getDiscountPercent())
                .initialFormatted(price.getInitialFormatted())
                .finalFormatted(price.getFinalFormatted())
                .priceAvailable(price.isPriceAvailable())
                .priceCheckedAt(price.getPriceCheckedAt())
                .build();
    }
}