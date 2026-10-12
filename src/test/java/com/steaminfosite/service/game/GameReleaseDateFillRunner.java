package com.steaminfosite.service.game;

import com.steaminfosite.SteamInfoSiteApplication;
import com.steaminfosite.dto.response.GameReleaseDateTarget;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.service.save.GameReleaseDateSaveService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class GameReleaseDateFillRunner {

    private static final int BATCH_SIZE = 500;

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

            GameRepository gameRepository =
                    context.getBean(GameRepository.class);

            GameReleaseDateSaveService saveService =
                    context.getBean(GameReleaseDateSaveService.class);

            long afterAppId = 0L;
            int totalReadCount = 0;
            int totalUpdatedCount = 0;

            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException(
                            "출시일 변환 작업이 중단되었습니다"
                    );
                }

                List<GameReleaseDateTarget> targets =
                        gameRepository.findReleaseDateTargetsAfter(
                                afterAppId,
                                PageRequest.of(0, BATCH_SIZE)
                        );

                if (targets.isEmpty()) {
                    break;
                }

                int updatedCount =
                        saveService.fillReleaseSortDates(targets);

                totalReadCount += targets.size();
                totalUpdatedCount += updatedCount;
                //변환하지 못한 값이 있어도 다음 게임으로 이동
                afterAppId = targets.get(targets.size() - 1).appId();

                System.out.println(
                        "출시일 변환 진행: 마지막 appId="
                                + afterAppId
                                + ", 이번 조회=" + targets.size()
                                + ", 이번 수정=" + updatedCount
                                + ", 누적 수정=" + totalUpdatedCount
                );
            }

            System.out.println(
                    "출시일 변환 종료: 조회="
                            + totalReadCount
                            + ", 수정=" + totalUpdatedCount
                            + ", 미변경="
                            + (totalReadCount - totalUpdatedCount)
            );
        }
    }
}