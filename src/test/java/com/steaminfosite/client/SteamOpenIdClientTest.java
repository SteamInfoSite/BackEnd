package com.steaminfosite.client;

import com.steaminfosite.config.SteamOpenIdProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamOpenIdClientTest {

    private SteamOpenIdClient steamOpenIdClient;
    private MockRestServiceServer mockServer;
    private SteamOpenIdProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SteamOpenIdProperties();
        properties.setLoginUrl("https://steamcommunity.com/openid/login");
        properties.setRealm("http://localhost:8080/");
        properties.setReturnUrl("http://localhost:8080/api/auth/steam/callback");
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        steamOpenIdClient = new SteamOpenIdClient(properties, builder.build());
    }

    @Test
    @DisplayName("스팀 검증 응답이 유효하면 true를 반환")
    void verifyCallback() {
        MultiValueMap<String, String> params = createCallbackParams();

        mockServer.expect(requestTo(properties.getLoginUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(
                        containsString(
                                "openid.mode=check_authentication"
                        )
                ))
                .andRespond(withSuccess(
                        "ns:http://specs.openid.net/auth/2.0\n"
                                + "is_valid:true\n",
                        MediaType.TEXT_PLAIN
                ));

        boolean result = steamOpenIdClient.verifyCallback(params);

        assertThat(result).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Steam 검증 응답이 유효하지 않으면 false를 반환한다")
    void verifyCallbackInvalid() {
        MultiValueMap<String, String> params = createCallbackParams();

        mockServer.expect(requestTo(properties.getLoginUrl()))
                .andRespond(withSuccess(
                        "is_valid:false\n",
                        MediaType.TEXT_PLAIN
                ));

        boolean result = steamOpenIdClient.verifyCallback(params);

        assertThat(result).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("callback 파라미터가 없으면 예외가 발생한다")
    void verifyCallbackEmpty() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        Throwable exception = catchThrowable(
                () -> steamOpenIdClient.verifyCallback(params)
        );

        assertThat(exception)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Steam OpenID callback 파라미터가 필요합니다");
    }

    private MultiValueMap<String, String> createCallbackParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("state", "test-state");
        params.add("openid.mode", "id_res");
        params.add(
                "openid.claimed_id",
                "https://steamcommunity.com/openid/id/76561198000000000"
        );

        return params;
    }
}