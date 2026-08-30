package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameInfoResponse;

public interface SteamApiService {
    GameInfoResponse getGameInfo(long appId);

    String getJsonDetails();

    GameInfoResponse parseJson(String gameData) throws Exception;
}
