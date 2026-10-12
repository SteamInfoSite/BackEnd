package com.steaminfosite.client;

import com.steaminfosite.config.RestClientConfig;
import org.springframework.web.client.RestClient;

import java.util.List;

public class SteamPriceApiManualCheck {

    public static void main(String[] args) {
        //기존 연결 응답 타임아웃 설정 사용
        RestClient restClient = new RestClientConfig()
                .steamRestClient(RestClient.builder());

        SteamPriceApiClient client =
                new SteamPriceApiClient(restClient);

        List<Long> appIds = List.of(620L, 570L);
        //두게임의 가격을 한 번에 요청
        var result = client.fetchPrices(appIds);

        for (Long appId : appIds) {
            System.out.println("게임 appId = " + appId);

            var wrapper = result.get(String.valueOf(appId));

            if (wrapper == null) {
                System.out.println("응답 누락");
                continue;
            }

            if (!Boolean.TRUE.equals(wrapper.getSuccess())) {
                System.out.println(
                        "조회 실패 또는 성공 여부 누락: "
                                + wrapper.getSuccess()
                );
                continue;
            }

            if (wrapper.getData() == null) {
                System.out.println("data 누락 또는 null");
                continue;
            }

            var price = wrapper.getData().getPriceOverview();

            if (price == null) {
                System.out.println("정상 응답: 가격 정보 미제공");
                continue;
            }

            System.out.println("통화 = " + price.getCurrency());
            System.out.println("정가 원본 = " + price.getInitialAmount());
            System.out.println("판매가 원본 = " + price.getFinalAmount());
            System.out.println("할인율 = " + price.getDiscountPercent());
            System.out.println("표시용 정가 = " + price.getInitialFormatted());
            System.out.println("표시용 판매가 = " + price.getFinalFormatted());
        }
    }
}