package com.steaminfosite.service.price;

import com.steaminfosite.SteamInfoSiteApplication;
import com.steaminfosite.repository.GamePriceRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public class GamePriceCollectManualCheck {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(
                             SteamInfoSiteApplication.class
                     )
                             .web(WebApplicationType.NONE)
                             .run("--steam.genre.refresh.enabled=false")) {

            GamePriceCollectService collectService =
                    context.getBean(GamePriceCollectService.class);

            GamePriceRepository repository =
                    context.getBean(GamePriceRepository.class);

            List<Long> appIds = List.of(620L, 570L);

            int savedCount = collectService.collectPrices(appIds);

            System.out.println("savedCount = " + savedCount);

            if (savedCount != appIds.size()) {
                throw new IllegalStateException(
                        "Some prices were not processed. Check the collection logs."
                );
            }

            var savedPrices = repository.findAllByAppIdIn(appIds);

            if (savedPrices.size() != appIds.size()) {
                throw new IllegalStateException(
                        "Saved price rows are missing."
                );
            }

            for (var price : savedPrices) {
                System.out.println(
                        "appId=" + price.getAppId()
                                + ", available=" + price.isPriceAvailable()
                                + ", currency=" + price.getCurrency()
                                + ", initial=" + price.getInitialAmount()
                                + ", final=" + price.getFinalAmount()
                                + ", discount=" + price.getDiscountPercent()
                                + ", checkedAt=" + price.getPriceCheckedAt()
                );
            }
        }
    }
}