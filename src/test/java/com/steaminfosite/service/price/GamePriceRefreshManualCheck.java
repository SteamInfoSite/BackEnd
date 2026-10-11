package com.steaminfosite.service.price;

import com.steaminfosite.SteamInfoSiteApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class GamePriceRefreshManualCheck {

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

            GamePriceRefreshService refreshService = context.getBean(GamePriceRefreshService.class);

            int savedCount = refreshService.refreshDefaultPrices();

            System.out.println(
                    "Price refresh savedCount = " + savedCount
            );
        }
    }
}