package com.steaminfosite.service.game;

import com.steaminfosite.dto.response.GameDetailCollectResult;

public interface GameReleaseRefreshService {
    GameDetailCollectResult refreshReleaseStatuses();
}