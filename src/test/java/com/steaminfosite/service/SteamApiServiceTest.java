package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.dto.response.GameDetailCollectResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SteamApiServiceTest {

    @Autowired
    SteamApiServiceImpl service;

    @Autowired
    SteamAppListService steamAppListService;

    @Autowired
    GameDetailCollectService gameDetailCollectService;

    @Test
    void gameDetailCollectAllTest() {
        GameDetailCollectResult result = gameDetailCollectService.collectAllNotFetched(1000);

        System.out.println("성공: " + result.successCount());
        System.out.println("미발견: " + result.notFoundCount());
        System.out.println("API 오류: " + result.apiErrorCount());
        System.out.println("429: " + result.rateLimitedCount());
        System.out.println("전체 요청 시도: " + result.attemptedCount());
    }

    @Test
    void gameDetailCollectTest() {
        GameDetailCollectResult result = gameDetailCollectService.collectNotFetched(100);

        System.out.println("성공: " + result.successCount());
        System.out.println("미발견: " + result.notFoundCount());
        System.out.println("API 오류: " + result.apiErrorCount());
        System.out.println("429: " + result.rateLimitedCount());
        System.out.println("전체 요청 시도: " + result.attemptedCount());
    }

    @Test
    void steamAppListTest() {
        int count = steamAppListService.steamAppList(1000);
        System.out.println("저장 또는 갱신한 앱 개수 = " + count);
    }

    @Test
    void appListTest() {    //실행 환경변수에 STEAM_API_KEY=키
        int count = steamAppListService.collectSteamAppList(10, 1);
        System.out.println("가져온 앱 개수 = " + count);
    }

    @Test
    void getGameInfo(){
        long appId = 4787850;
        GameInfoResponse gameData = service.getGameInfo(appId);
        System.out.println(gameData);
    }

}
