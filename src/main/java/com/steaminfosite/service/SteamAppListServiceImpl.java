package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.applist.SteamAppDto;
import com.steaminfosite.dto.applist.SteamAppList;
import com.steaminfosite.service.save.SteamAppSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SteamAppListServiceImpl implements SteamAppListService{
    private final SteamApiClient steamApiClient;
    private final SteamAppSaveService steamAppSaveService;

    @Override
    public int collectSteamAppList(int maxResults, int maxPages) {
        Long lastAppId = null;
        int totalCount = 0;
        int pageCount = 0;

        while (pageCount < maxPages) {
            SteamAppList appList = steamApiClient.fetchAppList(lastAppId, maxResults);
            pageCount++;

            List<SteamAppDto> apps = appList.getApps();

            if (apps != null && !apps.isEmpty()) {
                totalCount += apps.size();

                for (SteamAppDto app : apps) {
                    System.out.println(app.getAppId() + " / " + app.getName());
                }
            }

            if (!Boolean.TRUE.equals(appList.getHaveMoreResults())) {
                break;  //have_more_results없을시 종료
            }

            Long nextLastAppId = appList.getLastAppId();

            if (nextLastAppId == null || nextLastAppId.equals(lastAppId)) {
                break;
            }

            lastAppId = nextLastAppId;
        }

        return totalCount;
    }

    @Override
    public int steamAppList(int maxResults) {   //범위 이내 구간 순회
        Long lastAppId = null;
        int totalCount = 0;

        while (true) {
            //steam에서 한페이지 가져오기
            SteamAppList appList = steamApiClient.fetchAppList(lastAppId, maxResults);
            List<SteamAppDto> apps = appList.getApps();

            //가져온 페이지 db저장
            if (apps != null && !apps.isEmpty()) {
                int savedCount = steamAppSaveService.saveOrUpdateSteamApps(apps);
                totalCount += savedCount;
            }
            //다음 페이지가 없으면 종료
            if (!Boolean.TRUE.equals(appList.getHaveMoreResults())) {
                break;
            }

            //다음 페이지 기준 appId 준비
            Long nextLastAppId = appList.getLastAppId();

            if (nextLastAppId == null || nextLastAppId.equals(lastAppId)) {
                break;  //last_appid가 같은 값 혹은 아예 안나와서 없을 경우 순회 끝
            }
            lastAppId = nextLastAppId;
        }
        return totalCount;
    }
}
