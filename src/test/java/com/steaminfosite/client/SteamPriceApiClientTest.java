package com.steaminfosite.client;

import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamRateLimitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamPriceApiClientTest {

    private static final String BASE_URL = "https://store.steampowered.com";

    private static final String REQUEST_URL =
            BASE_URL
                    + "/api/appdetails"
                    + "?appids=620,570"
                    + "&filters=price_overview"
                    + "&cc=kr"
                    + "&l=english";

    private MockRestServiceServer mockServer;
    private SteamPriceApiClient steamPriceApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);

        mockServer = MockRestServiceServer.bindTo(builder).build();

        steamPriceApiClient = new SteamPriceApiClient(
                builder.build()
        );
    }

    @AfterEach
    void verify() {
        mockServer.verify();
    }

    @Test
    @DisplayName("가격 객체와 빈 배열 응답을 함께 변환")
    void convertPriceAndEmptyArray() {
        //변환 검증용 고정 데이터이며 현재 판매 가격을 의미하지 않음
        String responseBody = """
                {
                  "620": {
                    "success": true,
                    "data": {
                      "price_overview": {
                        "currency": "KRW",
                        "initial": 1100000,
                        "final": 220000,
                        "discount_percent": 80,
                        "initial_formatted": "₩11,000",
                        "final_formatted": "₩2,200"
                      }
                    }
                  },
                  "570": {
                    "success": true,
                    "data": []
                  }
                }
                """;

        mockServer.expect(requestTo(REQUEST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        var result = steamPriceApiClient.fetchPrices(
                List.of(620L, 570L)
        );

        assertThat(result).containsOnlyKeys("620", "570");

        var paidGame = result.get("620");

        assertThat(paidGame.getSuccess()).isTrue();
        assertThat(paidGame.getData()).isNotNull();

        var price = paidGame.getData().getPriceOverview();

        assertThat(price).isNotNull();
        assertThat(price.getCurrency()).isEqualTo("KRW");
        assertThat(price.getInitialAmount()).isEqualTo(1100000L);
        assertThat(price.getFinalAmount()).isEqualTo(220000L);
        assertThat(price.getDiscountPercent()).isEqualTo(80);
        assertThat(price.getInitialFormatted()).isEqualTo("₩11,000");
        assertThat(price.getFinalFormatted()).isEqualTo("₩2,200");

        var noPriceGame = result.get("570");

        assertThat(noPriceGame.getSuccess()).isTrue();
        assertThat(noPriceGame.getData()).isNotNull();
        assertThat(noPriceGame.getData().getPriceOverview()).isNull();
    }

    @Test
    @DisplayName("내용이 있는 배열은 가격 없음으로 처리하지 않는다")
    void rejectNonEmptyArray() {
        String responseBody = """
                {
                  "620": {
                    "success": true,
                    "data": [123]
                  }
                }
                """;

        mockServer.expect(requestTo(REQUEST_URL))
                .andRespond(withSuccess(
                        responseBody,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(
                () -> steamPriceApiClient.fetchPrices(
                        List.of(620L, 570L)
                )
        ).isInstanceOf(SteamApiException.class);
    }

    @Test
    @DisplayName("429 응답은 요청 제한 예외로 전달한다")
    void handleRateLimit() {
        mockServer.expect(requestTo(REQUEST_URL))
                .andRespond(withStatus(
                        HttpStatus.TOO_MANY_REQUESTS
                ));

        assertThatThrownBy(
                () -> steamPriceApiClient.fetchPrices(
                        List.of(620L, 570L)
                )
        ).isInstanceOf(SteamRateLimitException.class);
    }
}