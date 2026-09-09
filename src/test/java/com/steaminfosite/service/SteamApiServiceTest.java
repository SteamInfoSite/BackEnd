package com.steaminfosite.service;

import com.steaminfosite.dto.request.GameInfoRequest;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class SteamApiServiceTest {

    @Autowired
    SteamApiServiceImpl service;

    @Autowired
    SteamAppListService steamAppListService;

    @Autowired
    GameDetailCollectService gameDetailCollectService;

    @Test
    void gameDetailCollectTest(){
        int count = gameDetailCollectService.collectNotFetched(10);
        System.out.println("상세정보 처리 개수 : " + count);
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
