package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceResponse;
import com.steaminfosite.entity.price.GamePriceEntity;
import com.steaminfosite.mapper.GamePriceResponseMapper;
import com.steaminfosite.repository.GamePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GamePriceSearchServiceImpl
        implements GamePriceSearchService {

    private final GamePriceRepository gamePriceRepository;
    private final GamePriceResponseMapper gamePriceResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, GamePriceResponse> getPricesByAppIds(
            Collection<Long> appIds
    ) {
        if (appIds == null) {
            throw new IllegalArgumentException(
                    "게임 ID 목록이 필요합니다"
            );
        }

        if (appIds.isEmpty()) {
            return Map.of();
        }

        if (appIds.stream().anyMatch(
                appId -> appId == null || appId <= 0
        )) {
            throw new IllegalArgumentException(
                    "올바른 스팀 게임 ID가 필요합니다"
            );
        }

        List<Long> uniqueAppIds = appIds.stream()
                .distinct()
                .toList();

        List<GamePriceEntity> savedPrices =
                gamePriceRepository.findAllByAppIdIn(uniqueAppIds);

        Map<Long, GamePriceResponse> responses = new HashMap<>();

        for (GamePriceEntity price : savedPrices) {
            responses.put(
                    price.getAppId(),
                    gamePriceResponseMapper.toResponse(price)
            );
        }

        return responses;
    }
}