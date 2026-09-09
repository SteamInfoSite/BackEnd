package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.response.GameInfoResponse;

import java.util.Optional;

public interface GameInfoSaveService {
    Optional<GameInfoResponse> findSavedGameInfo(Long appId);

    GameInfoResponse saveOrUpdateGameInfo(SteamApiData gameData);
}
