package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceCollectResult;
import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.price.GamePriceEntity;
import com.steaminfosite.repository.GamePriceRepository;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.repository.SteamAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GamePriceRefreshServiceImpl
        implements GamePriceRefreshService {

    private static final int DEFAULT_TARGET_COUNT = 100;
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);
    private static final int MISSING_PRICE_BATCH_SIZE = 20;
    private static final long PRICE_RETRY_DELAY_MINUTES = 60;
    private static final int PRICE_CHANGE_BATCH_SIZE = 10;
    private static final int DISCOUNT_REFRESH_BATCH_SIZE = 10;

    private final SteamAppRepository steamAppRepository;

    private final GameRepository gameRepository;
    private final GamePriceRepository gamePriceRepository;
    private final GamePriceCollectService gamePriceCollectService;

    @Override
    public int refreshDefaultPrices() {
        //출시된 유료 게임 중 추천 수 상위 100개의 ID 조회
        List<Long> targetAppIds =
                gameRepository.findPriceCollectionTargetAppIds(
                        PageRequest.of(0, DEFAULT_TARGET_COUNT)
                );

        if (targetAppIds.isEmpty()) {
            return 0;
        }
        //대상 게임들의 저장된 가격을 한 번에 조회
        Map<Long, GamePriceEntity> savedPrices = new HashMap<>();

        for (GamePriceEntity price :
                gamePriceRepository.findAllByAppIdIn(targetAppIds)) {
            savedPrices.put(price.getAppId(), price);
        }
        LocalDateTime refreshBefore = LocalDateTime.now().minus(REFRESH_INTERVAL);
        // 조회 기록이 없거나 마지막 확인 후 1시간이 지난 게임 선택
        List<Long> refreshAppIds = targetAppIds.stream()
                .filter(appId -> {
                    GamePriceEntity price = savedPrices.get(appId);

                    return price == null
                            || price.getPriceCheckedAt() == null
                            || !price.getPriceCheckedAt()
                            .isAfter(refreshBefore);
                })
                .toList();

        System.out.println(
                "가격 갱신 대상 확인: 기본 대상="
                        + targetAppIds.size()
                        + ", 갱신 필요="
                        + refreshAppIds.size()
        );

        if (refreshAppIds.isEmpty()) {
            return 0;
        }

        return gamePriceCollectService
                .collectPrices(refreshAppIds)
                .savedCount();
    }

    @Override
    public GamePriceCollectResult collectMissingPrices() {
        List<Long> appIds = gameRepository.findMissingPriceAppIdsNewestFirst(
                LocalDateTime.now(),
                PageRequest.of(0, MISSING_PRICE_BATCH_SIZE)
        );

        return collectWithRetryDelay(appIds);
    }

    @Override
    public GamePriceCollectResult refreshChangedPrices() {
        List<Long> appIds = gameRepository.findPriceChangeRefreshAppIds(
                LocalDateTime.now(),
                PageRequest.of(0, PRICE_CHANGE_BATCH_SIZE)
        );

        return collectWithRetryDelay(appIds);
    }

    private GamePriceCollectResult collectWithRetryDelay(List<Long> appIds) {
        if (appIds.isEmpty()) {
            return new GamePriceCollectResult(0, false);
        }

        List<SteamAppEntity> apps = steamAppRepository.findAllByAppIdIn(appIds);

        for (SteamAppEntity app : apps) {
            app.deferPriceAttempt(PRICE_RETRY_DELAY_MINUTES);
        }

        steamAppRepository.saveAll(apps);
        return gamePriceCollectService.collectPrices(appIds);
    }

    @Override
    public GamePriceCollectResult refreshStaleDiscountPrices() {
        LocalDateTime now = LocalDateTime.now();

        List<Long> appIds = gameRepository.findStaleDiscountPriceAppIds(
                now,
                now.minus(REFRESH_INTERVAL),
                PageRequest.of(0, DISCOUNT_REFRESH_BATCH_SIZE)
        );

        return collectWithRetryDelay(appIds);
    }
}