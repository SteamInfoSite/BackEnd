package com.steaminfosite.service;
import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.service.save.GameInfoSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SteamApiServiceImpl implements SteamApiService {

    private final SteamApiClient steamApiClient;
    private final GameInfoSaveService gameInfoSaveService;

    @Override
    public GameInfoResponse getGameInfo(long appId){
        //DB에 이미 저장된 상세정보가 있으면 Steam API를 호출하지 않고 반환
        Optional<GameInfoResponse> savedGame = gameInfoSaveService.findSavedGameInfo(appId);
        if(savedGame.isPresent()){
            return savedGame.get();
        }
        //DB에 없으면 Steam API에서 상세정보를 받아옴
        Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(appId);
        if (gameData.isEmpty()){
            throw (new IllegalArgumentException("게임 데이터를 받아올 수 없습니다"));
        }
        //받아온 상세정보를 DB에 저장하고 응답 DTO로 반환
        return gameInfoSaveService.saveOrUpdateGameInfo(gameData.get());
    }

}
