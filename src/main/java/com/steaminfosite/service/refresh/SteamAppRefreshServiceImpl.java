package com.steaminfosite.service.refresh;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.applist.SteamAppDto;
import com.steaminfosite.dto.applist.SteamAppList;
import com.steaminfosite.entity.refresh.SteamAppRefreshStateEntity;
import com.steaminfosite.repository.SteamAppRefreshStateRepository;
import com.steaminfosite.service.ratelimit.SteamRequestRateLimiter;
import com.steaminfosite.service.save.SteamAppSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteamAppRefreshServiceImpl
        implements SteamAppRefreshService {

    private static final int PAGE_SIZE = 1000;
    private static final long OVERLAP_SECONDS = 300;

    private final SteamApiClient steamApiClient;
    private final SteamAppSaveService steamAppSaveService;
    private final SteamAppRefreshStateRepository refreshStateRepository;
    private final SteamRequestRateLimiter rateLimiter;

    @Override
    public synchronized int refreshAppList() {
        SteamAppRefreshStateEntity state = refreshStateRepository
                .findById(SteamAppRefreshStateEntity.APP_LIST_STATE_ID)
                .orElseGet(
                        SteamAppRefreshStateEntity::createInitialState
                );

        Instant startedAt = Instant.now();
        Instant previousStartedAt = state.getLastSuccessfulStartedAt();
        //이전 확인 시각보다 5분 앞에서부터 다시 확인
        Instant modifiedSince = previousStartedAt == null
                ? null
                : Instant.ofEpochSecond(
                Math.max(
                        0,
                        previousStartedAt.getEpochSecond()
                                - OVERLAP_SECONDS
                )
        );

        Long lastAppId = null;
        int processedCount = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(
                        "앱 목록 갱신 작업이 중단되었습니다"
                );
            }

            rateLimiter.waitBeforeRequest();

            SteamAppList appList = steamApiClient.fetchAppList(
                    lastAppId,
                    PAGE_SIZE,
                    modifiedSince
            );

            List<SteamAppDto> apps = appList.getApps();
            //정상적인 빈 목록과 응답 필드 누락을 구분
            if (apps == null) {
                throw new IllegalStateException(
                        "스팀 앱 목록 응답에 apps가 없습니다"
                );
            }

            boolean hasMore = Boolean.TRUE.equals(appList.getHaveMoreResults());

            Long nextLastAppId = appList.getLastAppId();
            //다음 페이지가 있다고 했는데 진행할 수 없으면 실패 처리
            if (hasMore && (
                    apps.isEmpty()
                            || nextLastAppId == null
                            || nextLastAppId <= 0
                            || (lastAppId != null
                            && nextLastAppId <= lastAppId)
            )) {
                throw new IllegalStateException(
                        "스팀 앱 목록의 다음 페이지 정보가 올바르지 않습니다"
                );
            }

            if (!apps.isEmpty()) {
                processedCount += steamAppSaveService.saveOrUpdateSteamApps(apps);
            }

            if (!hasMore) {
                break;
            }

            lastAppId = nextLastAppId;
        }
        //모든 페이지의 저장이 끝난 뒤에만 기준 시각 변경
        state.markCompleted(startedAt);
        refreshStateRepository.save(state);

        log.info(
                "앱 목록 갱신 완료: 조회 기준={}, 처리={}개, 새 기준={}",
                modifiedSince,
                processedCount,
                startedAt
        );

        return processedCount;
    }
}