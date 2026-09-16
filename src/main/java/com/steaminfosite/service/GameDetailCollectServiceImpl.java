package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.response.GameDetailCollectResult;
import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;
import com.steaminfosite.repository.SteamAppRepository;
import com.steaminfosite.service.save.GameInfoSaveService;
import com.steaminfosite.service.ratelimit.SteamRequestRateLimiter;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamRateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameDetailCollectServiceImpl implements GameDetailCollectService{
    private static final int SAVE_BATCH_SIZE = 100;
    private static final long API_ERROR_RETRY_DELAY_MINUTES = 10;
    private static final int MAX_API_ERROR_RETRY_COUNT = 3;
    private final SteamAppRepository steamAppRepository;
    private final SteamApiClient steamApiClient;
    private final GameInfoSaveService gameInfoSaveService;
    private final SteamRequestRateLimiter rateLimiter;

    @Override
    public GameDetailCollectResult collectNotFetched(int limit) {
        //신규 수집 갱신 재시도 대상을 appId오름차순으로 limit개 조회
        List<SteamAppEntity> apps = steamAppRepository.findCollectTargets(
                List.of(
                        GameDetailStatus.NOT_FETCHED,
                        GameDetailStatus.UPDATE_REQUIRED,
                        GameDetailStatus.RATE_LIMITED
                ),
                GameDetailStatus.API_ERROR,
                MAX_API_ERROR_RETRY_COUNT,
                LocalDateTime.now(),
                PageRequest.of(0, limit)
        );

        int successCount = 0;
        int notFoundCount = 0;
        int apiErrorCount = 0;
        int rateLimitedCount = 0;

        //상세정보와 변경된 앱 상태를 저장 전까지 임시 보관
        List<SteamApiData> pendingGameData = new ArrayList<>(SAVE_BATCH_SIZE);
        List<SteamAppEntity> pendingStatusApps = new ArrayList<>(SAVE_BATCH_SIZE);

        for (SteamAppEntity app : apps) {
            try {
                //모든 상세 API요청이 전역 요청 간격을 따르도록 대기
                rateLimiter.waitBeforeRequest();

                Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(app.getAppId());

                //success=false도 HTTP요청 자체는 정상 처리라 성공으로 기록
                rateLimiter.recordSuccess();

                if (gameData.isPresent()) {
                    //즉시 저장하지 않고 묶음 저장 목록에 추가
                    pendingGameData.add(gameData.get());
                    app.updateDetailStatus(GameDetailStatus.SUCCESS);
                    successCount++;
                } else {
                    app.updateDetailStatus(GameDetailStatus.NOT_FOUND);
                    notFoundCount++;
                }
            } catch (SteamRateLimitException e) {
                //429발생시 요청간격과 다음요청 가능시각을 증가
                rateLimiter.recordRateLimited(e.getRetryAfterMillis());

                app.updateDetailStatus(GameDetailStatus.RATE_LIMITED);
                //429가 첫 요청에서 발생해도 전체 수집 반복이 종료되지 않도록 시도 횟수에 포함
                rateLimitedCount++;
                //429 직전까지 모은 데이터와 현재 상태를 먼저 저장
                pendingStatusApps.add(app);
                savePendingBatch(pendingGameData, pendingStatusApps);
                break;
            } catch (SteamApiException e) {
                //일시적인 API/네트워크 오류횟수와 다음 재시도 가능시각 기록
                app.recordApiError(API_ERROR_RETRY_DELAY_MINUTES);
                apiErrorCount++;
            }
            //정상,내용 없음,API오류 상태를 묶음 저장목록에 추가
            pendingStatusApps.add(app);
            //상태 변경앱이 저장갯수만큼 모이면 상세정보와 상태를 함께 저장
            if (pendingStatusApps.size() >= SAVE_BATCH_SIZE) {
                savePendingBatch(pendingGameData, pendingStatusApps);
            }
        }
        //50개가 채워지지 않고 남은 마지막 데이터도 저장
        savePendingBatch(pendingGameData, pendingStatusApps);
        return new GameDetailCollectResult(
                successCount,
                notFoundCount,
                apiErrorCount,
                rateLimitedCount
        );
    }

    @Override
    public GameDetailCollectResult collectAllNotFetched(int limit) {
        GameDetailCollectResult totalResult = GameDetailCollectResult.empty();
        int batchNumber = 0;

        while (true) {
            GameDetailCollectResult batchResult  = collectNotFetched(limit);

            //조회되거나 요청된 앱이 하나도 없으면 전체 수집 종료
            if (batchResult.attemptedCount() == 0) {
                break;
            }
            totalResult = totalResult.add(batchResult);
            batchNumber++;

            //묶음 처리가 끝날때마다 현재까지의 누적결과
            System.out.println(
                    "Steam 상세정보 수집 진행"
                            + "\n 반복=" + batchNumber
                            + "\n 성공=" + totalResult.successCount()
                            + "\n 미발견=" + totalResult.notFoundCount()
                            + "\n API 오류=" + totalResult.apiErrorCount()
                            + "\n 429=" + totalResult.rateLimitedCount()
                            + "\n 전체 시도=" + totalResult.attemptedCount()
                            + "\n 현재 요청 간격="
                            + rateLimiter.getCurrentDelayMillis()
                            + "ms"
            );
        }
        return totalResult;
    }
    //메모리에 모은 상세정보를 먼저 저장한 뒤 해당 앱들의 처리 상태를 저장
    private void savePendingBatch(
            List<SteamApiData> pendingGameData,
            List<SteamAppEntity> pendingStatusApps
    ) {
        //저장할 데이터가 없으면 DB를 호출하지 않고 종료
        if (pendingGameData.isEmpty() && pendingStatusApps.isEmpty()) {
            return;
        }
        System.out.println(
                "DB 묶음 저장" + "\n상세정보=" + pendingGameData.size()
                        + " | 앱 상태=" + pendingStatusApps.size()
        );
        //상세정보 저장이 성공한 뒤 상태를 저장해 SUCCESS만 남는 불일치를 방지
        if (!pendingGameData.isEmpty()) {
            gameInfoSaveService.saveOrUpdateGameInfo(pendingGameData);
        }

        if (!pendingStatusApps.isEmpty()) {
            steamAppRepository.saveAll(pendingStatusApps);
        }
        //DB 데이터가 아니라 다음 묶음을 담기 위해 메모리의 임시 목록만 비움
        pendingGameData.clear();
        pendingStatusApps.clear();
    }
}
