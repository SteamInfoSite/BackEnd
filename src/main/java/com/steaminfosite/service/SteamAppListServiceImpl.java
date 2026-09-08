package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.applist.SteamAppDto;
import com.steaminfosite.dto.applist.SteamAppList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SteamAppListServiceImpl implements SteamAppListService{
    private final SteamApiClient steamApiClient;

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
}
