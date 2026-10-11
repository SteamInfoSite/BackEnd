package com.steaminfosite.service.price;

import com.steaminfosite.client.SteamPriceApiClient;
import com.steaminfosite.dto.api.price.SteamPriceWrapper;
import com.steaminfosite.dto.response.GamePriceCollectResult;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamRateLimitException;
import com.steaminfosite.service.ratelimit.SteamRequestRateLimiter;
import com.steaminfosite.service.save.GamePriceSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GamePriceCollectServiceImpl
        implements GamePriceCollectService {

    private final SteamPriceApiClient steamPriceApiClient;
    private final GamePriceSaveService gamePriceSaveService;
    private final SteamRequestRateLimiter rateLimiter;

    @Override
    public GamePriceCollectResult collectPrices(Collection<Long> appIds) {
        if (appIds == null) {
            throw new IllegalArgumentException(
                    "게임 ID 목록이 필요합니다"
            );
        }

        if (appIds.isEmpty()) {
            return new GamePriceCollectResult(0, false);
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

        int savedCount = 0;
        boolean rateLimited = false;
        int batchSize = SteamPriceApiClient.MAX_BATCH_SIZE;

        for (int start = 0; start < uniqueAppIds.size();) {
            int end = Math.min(
                    start + batchSize,
                    uniqueAppIds.size()
            );

            List<Long> batch = uniqueAppIds.subList(start, end);

            Map<String, SteamPriceWrapper> responses;

            try {
                //기존 상세 수집기와 같은 요청 간격 관리 사용
                rateLimiter.waitBeforeRequest();

                responses = steamPriceApiClient.fetchPrices(batch);
                //HTTP 요청과 응답 변환이 정상 완료됨
                rateLimiter.recordSuccess();

            } catch (SteamRateLimitException e) {
                rateLimiter.recordRateLimited(
                        e.getRetryAfterMillis()
                );

                System.out.println(
                        "가격 수집 중단: Steam 요청 제한 appIds="
                                + batch
                );
                rateLimited = true;
                //이번 실행은 중단하고 기존 저장 가격 유지
                break;

            } catch (SteamApiException e) {
                System.out.println(
                        "가격 수집 실패: appIds="
                                + batch
                                + ", 원인="
                                + e.getMessage()
                );
                //실패한 묶음은 저장하지 않고 다음 묶음 진행
                start = end;
                continue;
            }
            //외부 요청이 끝난 뒤 저장 서비스의 트랜잭션 실행
            savedCount += gamePriceSaveService.saveOrUpdatePrices(
                    batch,
                    responses
            );

            start = end;
        }

        System.out.println(
                "가격 수집 종료: 대상="
                        + uniqueAppIds.size()
                        + ", 정상 저장 처리="
                        + savedCount
        );

        return new GamePriceCollectResult(savedCount, rateLimited);
    }
}