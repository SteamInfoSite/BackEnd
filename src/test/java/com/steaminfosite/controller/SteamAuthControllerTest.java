package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SteamAuthControllerTest {

    @Mock
    private SteamOpenIdClient steamOpenIdClient;

    @InjectMocks
    private SteamAuthController steamAuthController;

    @Test
    @DisplayName("스팀로그인 요청시 state를 세션에 저장후 로그인주소로 리다이렉트")
    void startLogin() {
        MockHttpSession session = new MockHttpSession();
        URI steamLoginUri = URI.create("https://steamcommunity.com/openid/login");
        given(steamOpenIdClient.createLoginUri(anyString())).willReturn(steamLoginUri);

        ResponseEntity<Void> response = steamAuthController.startLogin(session);

        String savedState = (String) session.getAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY
        );
        assertThat(savedState).isNotBlank();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(steamLoginUri);

        then(steamOpenIdClient).should().createLoginUri(savedState);
    }
    @Test
    @DisplayName("스팀 로그인 주소생성에 실패하면 예외전달")
    void startLoginFailure() {
        MockHttpSession session = new MockHttpSession();
        given(steamOpenIdClient.createLoginUri(anyString()))
                .willThrow(new IllegalStateException("스팀OpenID 주소 생성 실패"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> steamAuthController.startLogin(session)
        );

        assertThat(exception.getMessage()).isEqualTo("스팀OpenID 주소 생성 실패");
        then(steamOpenIdClient).should().createLoginUri(anyString());
    }
}