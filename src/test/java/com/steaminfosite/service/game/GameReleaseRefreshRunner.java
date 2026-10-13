package com.steaminfosite.service.game;

import com.steaminfosite.SteamInfoSiteApplication;
import com.steaminfosite.dto.response.GameDetailCollectResult;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class GameReleaseRefreshRunner {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                SteamInfoSiteApplication.class
        )
                .web(WebApplicationType.NONE)
                .run(
                        "--steam.genre.refresh.enabled=false",
                        "--steam.price.refresh.enabled=false"
                )) {
            GameReleaseRefreshService refreshService = context.getBean(
                    GameReleaseRefreshService.class
            );
            GameDetailCollectResult result = refreshService.refreshReleaseStatuses();

            System.out.println(
                    "상세정보 갱신 성공 = " + result.successCount()
            );
            System.out.println(
                    "미발견 = " + result.notFoundCount()
            );
            System.out.println(
                    "API 오류 = " + result.apiErrorCount()
            );
            System.out.println(
                    "요청 제한 발생 횟수 = " + result.rateLimitedCount()
            );
        }
    }
}