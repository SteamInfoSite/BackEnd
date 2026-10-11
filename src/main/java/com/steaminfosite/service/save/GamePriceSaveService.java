package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.price.SteamPriceWrapper;

import java.util.Collection;
import java.util.Map;

public interface GamePriceSaveService {

    int saveOrUpdatePrices(
            Collection<Long> requestedAppIds,
            Map<String, SteamPriceWrapper> responses
    );
}