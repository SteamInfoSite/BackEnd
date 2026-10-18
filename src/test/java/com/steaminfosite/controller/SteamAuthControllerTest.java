package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import com.steaminfosite.security.RefreshTokenCookieService;
import com.steaminfosite.security.SteamLoginStateCookieService;
import com.steaminfosite.service.user.RefreshTokenService;
import com.steaminfosite.service.user.SiteUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SteamAuthControllerTest {

    @Mock
    private SteamOpenIdClient steamOpenIdClient;

    @Mock
    private SiteUserService siteUserService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;

    @Spy
    private SteamLoginStateCookieService steamLoginStateCookieService =
            new SteamLoginStateCookieService(false);

    @InjectMocks
    private SteamAuthController steamAuthController;

    @Test
    @DisplayName("로그인 시작 시 state 쿠키를 발급하고 Steam으로 이동")
    void startLogin() {
        URI loginUri = URI.create("https://steamcommunity.com/openid/login");
        given(steamOpenIdClient.createLoginUri(anyString()))
                .willReturn(loginUri);

        ResponseEntity<Void> response = steamAuthController.startLogin();

        ArgumentCaptor<String> stateCaptor =
                ArgumentCaptor.forClass(String.class);
        then(steamOpenIdClient).should()
                .createLoginUri(stateCaptor.capture());

        String state = stateCaptor.getValue();
        assertThat(state).isNotBlank();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(loginUri);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains(
                        SteamLoginStateCookieService.COOKIE_NAME + "=" + state,
                        "HttpOnly",
                        "SameSite=Lax",
                        "Path=/api/auth/steam",
                        "Max-Age=900"
                );
    }

    @Test
    @DisplayName("Steam 로그인 주소 생성이 실패하면 오류를 전달")
    void startLoginFailure() {
        given(steamOpenIdClient.createLoginUri(anyString()))
                .willThrow(new IllegalStateException("주소 생성 실패"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> steamAuthController.startLogin()
        );

        assertThat(exception.getMessage()).isEqualTo("주소 생성 실패");
    }

    @Test
    @DisplayName("쿠키와 콜백의 state가 다르면 인증 거부")
    void callbackStateMismatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MultiValueMap<String, String> params =
                createCallbackParams("different-state");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, "saved-state")
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        then(steamOpenIdClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("state 쿠키가 없으면 인증 거부")
    void callbackWithoutStateCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MultiValueMap<String, String> params =
                createCallbackParams("test-state");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, null)
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        then(steamOpenIdClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Steam OpenID 검증이 실패하면 인증 거부")
    void callbackVerificationFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MultiValueMap<String, String> params =
                createCallbackParams("test-state");
        given(steamOpenIdClient.verifyCallback(params))
                .willReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, "test-state")
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        then(steamOpenIdClient).should().verifyCallback(params);
        then(steamOpenIdClient).should(never()).extractSteamId(params);
    }

    @Test
    @DisplayName("검증된 응답에서 Steam ID를 추출하지 못하면 인증 거부")
    void callbackSteamIdFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MultiValueMap<String, String> params =
                createCallbackParams("test-state");
        given(steamOpenIdClient.verifyCallback(params))
                .willReturn(true);
        given(steamOpenIdClient.extractSteamId(params))
                .willThrow(new IllegalArgumentException());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, "test-state")
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private MultiValueMap<String, String> createCallbackParams(String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("state", state);
        params.add("openid.mode", "id_res");
        params.add(
                "openid.claimed_id",
                "https://steamcommunity.com/openid/id/76561198000000000"
        );
        return params;
    }
}