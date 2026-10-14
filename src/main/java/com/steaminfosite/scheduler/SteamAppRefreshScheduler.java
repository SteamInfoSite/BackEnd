package com.steaminfosite.scheduler;

import com.steaminfosite.dto.response.GameDetailCollectResult;
import com.steaminfosite.service.GameDetailCollectService;
import com.steaminfosite.service.refresh.SteamAppRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "steam.app.refresh",
        name = "enabled",
        havingValue = "true"
)
public class SteamAppRefreshScheduler {
    @Value("${steam.app.refresh.batch-size:100}")
    private int detailBatchSize;
    private static final Duration APP_LIST_INTERVAL =
            Duration.ofHours(1);
    private static final Duration APP_LIST_RETRY_INTERVAL =
            Duration.ofMinutes(5);

    private final SteamAppRefreshService steamAppRefreshService;
    private final GameDetailCollectService gameDetailCollectService;

    private Instant nextAppListCheckAt = Instant.EPOCH;

    @Scheduled(
            initialDelayString =
                    "${steam.app.refresh.initial-delay-ms:30000}",
            fixedDelayString =
                    "${steam.app.refresh.worker-delay-ms:60000}"
    )
    public void refreshGames() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        if (!Instant.now().isBefore(nextAppListCheckAt)) {
            try {
                log.info("자동 앱 목록 갱신 시작");

                int processedCount = steamAppRefreshService.refreshAppList();

                nextAppListCheckAt = Instant.now().plus(APP_LIST_INTERVAL);

                log.info(
                        "자동 앱 목록 갱신 완료: 처리={}개, 다음 확인={}",
                        processedCount,
                        nextAppListCheckAt
                );

            } catch (RuntimeException e) {
                nextAppListCheckAt = Instant.now().plus(APP_LIST_RETRY_INTERVAL);

                log.error(
                        "자동 앱 목록 갱신 실패: 다음 확인={}",
                        nextAppListCheckAt,
                        e
                );
                return;
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        try {
            GameDetailCollectResult result =
                    gameDetailCollectService.collectNotFetched(
                            detailBatchSize
                    );

            if (result.attemptedCount() == 0) {
                log.debug("자동 상세정보 수집: 현재 처리할 대상 없음");
                return;
            }

            log.info(
                    "자동 상세정보 수집 완료: 성공={}, 미발견={}, "
                            + "API 오류={}, 요청 제한={}, 전체 시도={}",
                    result.successCount(),
                    result.notFoundCount(),
                    result.apiErrorCount(),
                    result.rateLimitedCount(),
                    result.attemptedCount()
            );

        } catch (RuntimeException e) {
            log.error("자동 상세정보 수집 실패", e);
        }
    }
}