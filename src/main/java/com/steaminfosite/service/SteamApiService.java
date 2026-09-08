package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameInfoResponse;

public interface SteamApiService {
    GameInfoResponse getGameInfo(long appId);

}
