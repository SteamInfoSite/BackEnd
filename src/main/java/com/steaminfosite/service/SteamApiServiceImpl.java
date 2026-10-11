package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.service.price.GamePriceSearchService;
import com.steaminfosite.service.save.GameInfoSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SteamApiServiceImpl implements SteamApiService {

    private final SteamApiClient steamApiClient;
    private final GameInfoSaveService gameInfoSaveService;
    private final GamePriceSearchService gamePriceSearchService;

    @Override
    public GameInfoResponse getGameInfo(long appId) {
        Optional<GameInfoResponse> savedGame = gameInfoSaveService.findSavedGameInfo(appId);
        GameInfoResponse response;

        if (savedGame.isPresent()) {
            //저장된 상세 정보 사용
            response = savedGame.get();
        } else {
            //상세 정보가 없을 때만 기존 수집 처리 실행
            Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(appId);

            if (gameData.isEmpty()) {
                throw new IllegalArgumentException(
                        "게임 데이터를 받아올 수 없습니다"
                );
            }
            response = gameInfoSaveService.saveOrUpdateGameInfo(
                    gameData.get()
            );
        }
        //저장된 가격을 상세 응답에 연결
        Long gameAppId = response.getAppId();

        response.setPrice(
                gamePriceSearchService.getPricesByAppIds(
                        List.of(gameAppId)
                ).get(gameAppId)
        );

        return response;
    }
}