package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameDetailCollectResult;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.service.GameDetailCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameReleaseRefreshServiceImpl
        implements GameReleaseRefreshService {

    private static final int BATCH_SIZE = 100;

    private final GameRepository gameRepository;
    private final GameDetailCollectService gameDetailCollectService;

    @Override
    public GameDetailCollectResult refreshReleaseStatuses() {
        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Seoul")
        );
        long afterAppId = 0L;
        GameDetailCollectResult totalResult = GameDetailCollectResult.empty();

        while (true) {
            checkInterrupted();

            List<Long> targetAppIds = gameRepository.findReleaseCheckTargetAppIdsAfter(
                    today,
                    afterAppId,
                    PageRequest.of(0, BATCH_SIZE)
            );

            if (targetAppIds.isEmpty()) {
                break;
            }
            int processedOffset = 0;
            //현재 묶음을 끝내고 나서 다음 대상들을 조회
            while (processedOffset < targetAppIds.size()) {
                checkInterrupted();

                List<Long> remainingAppIds = targetAppIds.subList(
                        processedOffset,
                        targetAppIds.size()
                );
                GameDetailCollectResult result =
                        gameDetailCollectService.collectByAppIds(
                                remainingAppIds
                        );

                if (result.processedCount() == 0
                        && result.rateLimitedCount() == 0) {
                    throw new IllegalStateException(
                            "출시 정보 갱신 대상이 처리되지 않았습니다"
                    );
                }
                totalResult = totalResult.add(result);
                //429가 발생한 게임은 처리 개수에서 제외되므로
                //다음 호출에서 해당 게임부터 다시 시도
                processedOffset += result.processedCount();

                if (result.rateLimitedCount() > 0) {
                    log.warn(
                            "출시 정보 갱신 대기: 현재 묶음 처리={}/{}, 누적 429={}",
                            processedOffset,
                            targetAppIds.size(),
                            totalResult.rateLimitedCount()
                    );
                }
            }
            afterAppId = targetAppIds.get(
                    targetAppIds.size() - 1
            );
            log.info(
                    "출시 정보 갱신 진행: 마지막 appId={}, "
                            + "누적 수집 성공={}, 미발견={}, API 오류={}, 429={}",
                    afterAppId,
                    totalResult.successCount(),
                    totalResult.notFoundCount(),
                    totalResult.apiErrorCount(),
                    totalResult.rateLimitedCount()
            );
        }
        log.info(
                "출시 정보 갱신 종료: 수집 성공={}, 미발견={}, API 오류={}, 429={}",
                totalResult.successCount(),
                totalResult.notFoundCount(),
                totalResult.apiErrorCount(),
                totalResult.rateLimitedCount()
        );

        return totalResult;
    }

    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException(
                    "출시 정보 갱신 작업이 중단되었습니다"
            );
        }
    }
}