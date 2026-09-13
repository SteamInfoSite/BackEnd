package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;
import com.steaminfosite.repository.SteamAppRepository;
import com.steaminfosite.service.save.GameInfoSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameDetailCollectServiceImpl implements GameDetailCollectService{
    private final SteamAppRepository steamAppRepository;
    private final SteamApiClient steamApiClient;
    private final GameInfoSaveService gameInfoSaveService;

    @Override
    public int collectNotFetched(int limit) {
        //steam_app_entity에서 NOT_FETCHED 상태인 앱 limit개 조회
        List<SteamAppEntity> apps = steamAppRepository.findByDetailStatusInOrderByAppIdAsc(
                List.of(
                        GameDetailStatus.NOT_FETCHED,
                        GameDetailStatus.UPDATE_REQUIRED,
                        GameDetailStatus.RATE_LIMITED,
                        GameDetailStatus.API_ERROR
                ),
                PageRequest.of(0, limit)
        );

        int processedCount = 0;

        for(SteamAppEntity app : apps){
            try {
                //실제 게임 상세정보 호출
                Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(app.getAppId());

                if(gameData.isPresent()){
                    gameInfoSaveService.saveOrUpdateGameInfo(gameData.get());
                    app.updateDetailStatus(GameDetailStatus.SUCCESS);
                } else {
                    app.updateDetailStatus(GameDetailStatus.NOT_FOUND);
                }
            } catch (RuntimeException e){
                if ("RATE_LIMITED".equals(e.getMessage())) {
                    app.updateDetailStatus(GameDetailStatus.RATE_LIMITED);
                    steamAppRepository.save(app);
                    break;
                }
                app.updateDetailStatus(GameDetailStatus.API_ERROR);
            }
            steamAppRepository.save(app);
            processedCount++;

            try {   //Failed 방지용 쉬는 코드
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return processedCount;
    }

    @Override
    public int collectAllNotFetched(int limit) {
        int totalCount = 0;

        while (true) {
            int processedCount = collectNotFetched(limit);

            if (processedCount == 0) {
                break;
            }

            totalCount += processedCount;
        }

        return totalCount;
    }
}
