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

    @Test
    void jsonTest(){
        String jsonData = service.getJsonDetails();
        System.out.println(jsonData);
        assertThat(jsonData).isNotNull();
    }

    @Test
    void gameData() throws Exception {
        String jsonData = service.getJsonDetails();
        GameInfoResponse gameInfo = service.parseJson(jsonData);
        System.out.println(gameInfo.getAppId());
        System.out.println(gameInfo.getGameName());
    }
}
