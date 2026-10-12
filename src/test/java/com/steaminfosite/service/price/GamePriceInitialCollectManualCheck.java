package com.steaminfosite.service.price;

import com.steaminfosite.SteamInfoSiteApplication;
import com.steaminfosite.repository.GameRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class GamePriceInitialCollectManualCheck {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(
                             SteamInfoSiteApplication.class
                     )
                             .web(WebApplicationType.NONE)
                             .run(
                                     "--steam.genre.refresh.enabled=false",
                                     "--steam.price.refresh.enabled=false"
                             )) {

            GamePriceInitialCollectService collectService =
                    context.getBean(
                            GamePriceInitialCollectService.class
                    );

            GameRepository gameRepository = context.getBean(GameRepository.class);

            var result = collectService.collectInitialPrices();

            System.out.println(
                    "savedCount = " + result.savedCount()
            );
            System.out.println(
                    "rateLimited = " + result.rateLimited()
            );
            //종료 후 아직 정상 확인하지 못한 게임이 남았는지 조회
            List<Long> remainingAppIds =
                    gameRepository.findUncollectedPriceAppIdsAfter(
                            0L,
                            PageRequest.of(0, 5)
                    );

            System.out.println(
                    "hasRemaining = " + !remainingAppIds.isEmpty()
            );
            System.out.println(
                    "remainingSampleAppIds = " + remainingAppIds
            );
        }
    }
}