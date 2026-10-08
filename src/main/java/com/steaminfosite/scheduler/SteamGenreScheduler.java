package com.steaminfosite.scheduler;

import com.steaminfosite.service.SteamGenreListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "steam.genre.refresh",
        name = "enabled",
        havingValue = "true"
)
public class SteamGenreScheduler {

    private final SteamGenreListService steamGenreListService;

    @Scheduled(
            cron = "${steam.genre.refresh.cron:0 0 * * * *}",
            zone = "Asia/Seoul"
    )
    public void refreshGenres() {
        try {
            int processedCount =
                    steamGenreListService.collectSteamGenres();

            log.info(
                    "장르 목록 갱신 완료: 처리한 장르 {}개",
                    processedCount
            );
        } catch (RuntimeException e) {
            log.error("장르 목록 갱신 실패", e);
        }
    }
}