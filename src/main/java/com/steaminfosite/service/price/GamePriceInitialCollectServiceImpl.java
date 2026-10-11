package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceCollectResult;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamePriceInitialCollectServiceImpl
        implements GamePriceInitialCollectService {

    private static final int COLLECTION_SIZE = 100;

    private final GameRepository gameRepository;
    private final GamePriceCollectService gamePriceCollectService;

    @Override
    public GamePriceCollectResult collectInitialPrices() {
        long afterAppId = 0L;
        int totalSavedCount = 0;
        int rateLimitCount = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(
                        "가격 최초 수집 작업이 중단되었습니다"
                );
            }

            List<Long> appIds =
                    gameRepository.findUncollectedPriceAppIdsAfter(
                            afterAppId,
                            PageRequest.of(0, COLLECTION_SIZE)
                    );

            if (appIds.isEmpty()) {
                log.info(
                        "가격 최초 수집 순회 종료: 정상 저장={}, 요청 제한 횟수={}",
                        totalSavedCount,
                        rateLimitCount
                );

                return new GamePriceCollectResult(
                        totalSavedCount,
                        false
                );
            }

            GamePriceCollectResult result =
                    gamePriceCollectService.collectPrices(appIds);

            totalSavedCount += result.savedCount();
            //요청 제한이면 현재 위치를 유지하고 미처리 대상을 다시 조회
            if (result.rateLimited()) {
                rateLimitCount++;

                log.warn(
                        "가격 최초 수집 재시도 예정: 요청 제한 횟수={}, "
                                + "누적 정상 저장={}, 조회 기준 appId={}",
                        rateLimitCount,
                        totalSavedCount,
                        afterAppId
                );

                // 다음 실제 API 요청에서 기존 rateLimiter가 대기 처리
                continue;
            }
            //일부 게임이 실패해도 이번 실행에서는 다음 ID로 이동
            afterAppId = appIds.get(appIds.size() - 1);

            log.info(
                    "가격 최초 수집 진행: 마지막 appId={}, "
                            + "이번 대상={}, 이번 저장={}, 누적 저장={}",
                    afterAppId,
                    appIds.size(),
                    result.savedCount(),
                    totalSavedCount
            );
        }
    }
}