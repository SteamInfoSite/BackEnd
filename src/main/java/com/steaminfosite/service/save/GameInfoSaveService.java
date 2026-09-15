package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.response.GameInfoResponse;

import java.util.List;
import java.util.Optional;

public interface GameInfoSaveService {
    Optional<GameInfoResponse> findSavedGameInfo(Long appId);

    GameInfoResponse saveOrUpdateGameInfo(SteamApiData gameData);

    //Steam 상세정보 여러 개를 하나의 트랜잭션으로 묶어서 저장
    int saveOrUpdateGameInfo(List<SteamApiData> gameDataList);
}
