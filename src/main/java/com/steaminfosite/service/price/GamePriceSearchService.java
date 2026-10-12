package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceResponse;

import java.util.Collection;
import java.util.Map;

public interface GamePriceSearchService {
    Map<Long, GamePriceResponse> getPricesByAppIds(
            Collection<Long> appIds
    );
}