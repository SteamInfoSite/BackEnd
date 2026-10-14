package com.steaminfosite.scheduler;

import com.steaminfosite.dto.response.GamePriceCollectResult;
import com.steaminfosite.service.price.GamePriceRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "steam.price.refresh",
        name = "enabled",
        havingValue = "true"
)
public class GamePriceRefreshScheduler {
    private static final Duration RATE_LIMIT_PAUSE = Duration.ofMinutes(15);

    private final GamePriceRefreshService gamePriceRefreshService;
    private Instant nextPriceCheckAt = Instant.EPOCH;

    @Scheduled(
            initialDelayString = "${steam.price.refresh.initial-delay-ms:120000}",
            fixedDelayString = "${steam.price.refresh.worker-delay-ms:60000}"
    )
    public void refreshPrices() {
        if (Thread.currentThread().isInterrupted()
                || Instant.now().isBefore(nextPriceCheckAt)) {
            return;
        }

        try {
            GamePriceCollectResult missingResult =
                    gamePriceRefreshService.collectMissingPrices();
            log.info("자동 미수집 가격 확인 완료: 저장={}개",
                    missingResult.savedCount());

            if (missingResult.rateLimited()) {
                nextPriceCheckAt = Instant.now().plus(RATE_LIMIT_PAUSE);
                log.warn("자동 가격 수집 요청 제한: 다음 시도={}", nextPriceCheckAt);
                return;
            }

            GamePriceCollectResult changedResult =
                    gamePriceRefreshService.refreshChangedPrices();
            log.info("자동 변경 가격 확인 완료: 저장={}개",
                    changedResult.savedCount());

            if (changedResult.rateLimited()) {
                nextPriceCheckAt = Instant.now().plus(RATE_LIMIT_PAUSE);
                log.warn("자동 가격 수집 요청 제한: 다음 시도={}", nextPriceCheckAt);
                return;
            }

            GamePriceCollectResult discountResult =
                    gamePriceRefreshService.refreshStaleDiscountPrices();
            log.info("자동 할인 가격 재확인 완료: 저장={}개",
                    discountResult.savedCount());

            if (discountResult.rateLimited()) {
                nextPriceCheckAt = Instant.now().plus(RATE_LIMIT_PAUSE);
                log.warn("자동 가격 수집 요청 제한: 다음 시도={}", nextPriceCheckAt);
            }
        } catch (RuntimeException e) {
            log.error("자동 가격 확인 실패", e);
        }
    }
}