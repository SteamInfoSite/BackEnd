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
                List.of(GameDetailStatus.NOT_FETCHED, GameDetailStatus.UPDATE_REQUIRED),
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
                    app.updateDetailStatus(GameDetailStatus.FAILED);
                }
            } catch (RuntimeException e){
                app.updateDetailStatus((GameDetailStatus.FAILED));
            }
            steamAppRepository.save(app);
            processedCount++;
        }
        return processedCount;
    }
}
