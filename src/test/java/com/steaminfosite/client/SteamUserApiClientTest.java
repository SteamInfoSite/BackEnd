package com.steaminfosite.client;

import com.steaminfosite.exception.SteamApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamUserApiClientTest {
    private static final String BASE_URL = "https://api.steampowered.com";
    private static final String API_KEY = "test-api-key";
    private static final String STEAM_ID = "76561198000000000";

    private MockRestServiceServer mockServer;
    private SteamUserApiClient steamUserApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        steamUserApiClient = new SteamUserApiClient(builder.build());
    }

    @AfterEach
    void verify() {
        mockServer.verify();
    }

    @Test
    @DisplayName("스팀API가 정상 응답하면 API키가 유효")
    void validate() {
        mockServer.expect(requestTo(
                        BASE_URL
                                + "/ISteamUser/GetPlayerSummaries/v2/"
                                + "?key=" + API_KEY
                                + "&steamids=" + STEAM_ID
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{}",
                        MediaType.APPLICATION_JSON
                ));

        boolean result = steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("스팀API가 403을 반환하면 API키가 유효하지 않음")
    void rejectInvalidKey() {
        mockServer.expect(requestTo(
                        BASE_URL
                                + "/ISteamUser/GetPlayerSummaries/v2/"
                                + "?key=" + API_KEY
                                + "&steamids=" + STEAM_ID
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        boolean result = steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("스팀 서버 오류는 API키 오류로 처리하지 않음")
    void throwOnServerError() {
        mockServer.expect(requestTo(
                        BASE_URL
                                + "/ISteamUser/GetPlayerSummaries/v2/"
                                + "?key=" + API_KEY
                                + "&steamids=" + STEAM_ID
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        
        assertThatThrownBy(
                () -> steamUserApiClient.validateApiKey(
                        API_KEY,
                        STEAM_ID
                )
        )
                .isInstanceOf(SteamApiException.class)
                .hasMessage(
                        "스팀API키 검증 중 스팀서버 오류가 발생했습니다"
                );
    }
}