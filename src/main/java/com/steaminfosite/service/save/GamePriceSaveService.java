package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.price.SteamPriceWrapper;

import java.util.Collection;
import java.util.Map;

public interface GamePriceSaveService {
    default int saveOrUpdatePrices(
            Collection<Long> requestedAppIds,
            Map<String, SteamPriceWrapper> responses
    ) {
        return saveOrUpdatePrices(
                requestedAppIds,
                responses,
                Map.of()
        );
    }

    int saveOrUpdatePrices(
            Collection<Long> requestedAppIds,
            Map<String, SteamPriceWrapper> responses,
            Map<Long, Long> priceChangeNumbers
    );
}