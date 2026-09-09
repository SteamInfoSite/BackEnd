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
        List<SteamAppEntity> apps = steamAppRepository.findByDetailStatusOrderByAppIdAsc(
                GameDetailStatus.NOT_FETCHED, PageRequest.of(0, limit)
        );

        int processedCount = 0;

        for(SteamAppEntity app : apps){
            try {   //이미 game_info_entity에 있는경우 상세정보 재호출 안하고 SUCCESS 처리
                if(gameInfoSaveService.findSavedGameInfo(app.getAppId()).isPresent()){
                    app.updateDetailStatus(GameDetailStatus.SUCCESS);
                    steamAppRepository.save(app);
                    processedCount++;
                    continue;
                }
                //없으면 실제 게임 상세정보 호출
                Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(app.getAppId());

                if(gameData.isPresent()){
                    gameInfoSaveService.saveGameInfo(gameData.get());
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
