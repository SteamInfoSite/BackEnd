package com.steaminfosite.controller;

import com.steaminfosite.dto.request.SteamApiKeyRegisterRequest;
import com.steaminfosite.dto.response.SteamApiKeyStatusResponse;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.service.user.SteamApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SteamApiKeyControllerTest {

    private static final String STEAM_ID = "76561198000000000";
    private static final String API_KEY = "0123456789abcdef0123456789abcdef";

    @Mock
    private SteamApiKeyService steamApiKeyService;

    private SteamApiKeyController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        controller = new SteamApiKeyController(steamApiKeyService);
        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(STEAM_ID)
                .build();
    }

    @Test
    @DisplayName("JWT의 Steam ID로 API 키를 등록")
    void registerValidKey() {
        given(steamApiKeyService.register(STEAM_ID, API_KEY))
                .willReturn(SteamApiKeyStatus.VALID);

        ResponseEntity<SteamApiKeyStatusResponse> response =
                controller.register(new SteamApiKeyRegisterRequest(API_KEY), jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(SteamApiKeyStatus.VALID);
        assertThat(response.getBody().usable()).isTrue();
        then(steamApiKeyService).should().register(STEAM_ID, API_KEY);
    }

    @Test
    @DisplayName("유효하지 않은 API 키 등록 결과는 422")
    void rejectInvalidKey() {
        given(steamApiKeyService.register(STEAM_ID, API_KEY))
                .willReturn(SteamApiKeyStatus.INVALID);

        ResponseEntity<SteamApiKeyStatusResponse> response =
                controller.register(new SteamApiKeyRegisterRequest(API_KEY), jwt);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(SteamApiKeyStatus.INVALID);
        assertThat(response.getBody().usable()).isFalse();
    }

    @Test
    @DisplayName("JWT의 Steam ID로 API 키 상태를 조회")
    void getStatus() {
        given(steamApiKeyService.getStatus(STEAM_ID))
                .willReturn(SteamApiKeyStatus.NOT_REGISTERED);

        ResponseEntity<SteamApiKeyStatusResponse> response =
                controller.status(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(SteamApiKeyStatus.NOT_REGISTERED);
        assertThat(response.getBody().usable()).isFalse();
        then(steamApiKeyService).should().getStatus(STEAM_ID);
    }

    @Test
    @DisplayName("Steam API 오류를 서비스에서 전달")
    void propagateSteamApiError() {
        given(steamApiKeyService.register(STEAM_ID, API_KEY))
                .willThrow(new SteamApiException(
                        "스팀 서버 연결 오류",
                        new RuntimeException()
                ));

        assertThatThrownBy(() ->
                controller.register(new SteamApiKeyRegisterRequest(API_KEY), jwt)
        ).isInstanceOf(SteamApiException.class);
    }
}