package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import com.steaminfosite.dto.response.SteamLoginStatusResponse;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.service.user.SiteUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
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

    @Test
    @DisplayName("state와 스팀 검증이 유효하면 callback 요청에 성공")
    void callback() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY,
                "test-state"
        );
        MultiValueMap<String, String> params = createCallbackParams("test-state");
        given(steamOpenIdClient.verifyCallback(params)).willReturn(true);
        given(steamOpenIdClient.extractSteamId(params)).willReturn("76561198000000000");
        SiteUserEntity siteUser = new SiteUserEntity("76561198000000000");
        given(siteUserService.findOrCreate("76561198000000000")).willReturn(siteUser);

        ResponseEntity<String> response = steamAuthController.callback(params, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(
                "스팀OpenID 인증에 성공했습니다. " + "스팀64 ID: 76561198000000000"
                );
        assertThat(session.getAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY
        )).isNull();
        assertThat(session.getAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY
        )).isEqualTo("76561198000000000");
        then(steamOpenIdClient)
                .should()
                .verifyCallback(params);
        then(steamOpenIdClient)
                .should()
                .extractSteamId(params);
        then(siteUserService)
                .should()
                .findOrCreate("76561198000000000");
    }

    @Test
    @DisplayName("세션과 callback의 state가 다르면 인증에 실패한다")
    void callbackStateFailure() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY,
                "saved-state"
        );
        MultiValueMap<String, String> params = createCallbackParams("different-state");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, session)
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason())
                .isEqualTo("유효하지 않은 스팀 로그인 요청입니다");
        then(steamOpenIdClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("스팀OpenID 검증이 유효하지 않으면 인증에 실패한다")
    void callbackVerificationFailure() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY,
                "test-state"
        );
        MultiValueMap<String, String> params =
                createCallbackParams("test-state");
        given(steamOpenIdClient.verifyCallback(params))
                .willReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, session)
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason())
                .isEqualTo("스팀 로그인 인증에 실패했습니다");
        assertThat(session.getAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY
        )).isNull();
        assertThat(session.getAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY
        )).isNull();
        then(steamOpenIdClient).should().verifyCallback(params);
        then(steamOpenIdClient).should(never()).extractSteamId(params);
    }
    @Test
    @DisplayName("Steam64 ID를 추출할수 없으면 인증에 실패")
    void callbackSteamIdFailure() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY,
                "test-state"
        );
        MultiValueMap<String, String> params = createCallbackParams("test-state");
        given(steamOpenIdClient.verifyCallback(params)).willReturn(true);
        given(steamOpenIdClient.extractSteamId(params)).willThrow(new IllegalArgumentException());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> steamAuthController.callback(params, session)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("스팀 사용자 정보를 확인할 수 없습니다");
        assertThat(session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY)).isNull();
    }
    @Test
    @DisplayName("세션에 Steam64 ID가 있으면 로그인 상태를 반환")
    void statusLoggedIn() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY,
                "76561198000000000"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        ResponseEntity<SteamLoginStatusResponse> response = steamAuthController.status(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLoggedIn()).isTrue();
        assertThat(response.getBody().getSteamId()).isEqualTo("76561198000000000");
    }

    @Test
    @DisplayName("세션이 없으면 로그아웃 상태를 반환")
    void statusLoggedOut() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<SteamLoginStatusResponse> response = steamAuthController.status(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLoggedIn()).isFalse();
        assertThat(response.getBody().getSteamId()).isNull();
        //상태 조회 때문에 불필요한 세션이 만들어지지 않았는지 확인
        assertThat(request.getSession(false)).isNull();
    }
    @Test
    @DisplayName("로그아웃하면 로그인 세션을 만료")
    void logout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY,
                "76561198000000000"
        );
        request.setSession(session);

        ResponseEntity<String> response = steamAuthController.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("로그아웃되었습니다");
        assertThat(session.isInvalid()).isTrue();
    }
    //테스트 데이터
    private MultiValueMap<String, String> createCallbackParams(
            String state
    ) {
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