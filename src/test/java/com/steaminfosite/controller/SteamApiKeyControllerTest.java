package com.steaminfosite.controller;

import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.exception.GlobalExceptionHandler;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.service.user.SteamApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SteamApiKeyControllerTest {
    private static final String STEAM_ID = "76561198000000000";
    private static final String API_KEY = "0123456789abcdef0123456789abcdef";

    @Mock
    private SteamApiKeyService steamApiKeyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SteamApiKeyController controller = new SteamApiKeyController(steamApiKeyService);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("로그인 사용자는 유효한 스팀API키를 등록할 수 있음")
    void registerValidKey() throws Exception {
        MockHttpSession session = createLoginSession();
        given(steamApiKeyService.register(
                STEAM_ID,
                API_KEY
        )).willReturn(SteamApiKeyStatus.VALID);

        mockMvc.perform(
                put("/api/users/me/steam-api-key")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamApiKey": "%s"
                                }
                                """.formatted(API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.usable").value(true));
        then(steamApiKeyService).should().register(STEAM_ID, API_KEY);
    }

    @Test
    @DisplayName("유효하지 않은 스팀API키는 422를 반환")
    void rejectInvalidKey() throws Exception {
        MockHttpSession session = createLoginSession();
        given(steamApiKeyService.register(
                STEAM_ID,
                API_KEY
        )).willReturn(SteamApiKeyStatus.INVALID);

        mockMvc.perform(
                put("/api/users/me/steam-api-key")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamApiKey": "%s"
                                }
                                """.formatted(API_KEY)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.usable").value(false));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자의 API키 등록을 거부")
    void rejectAnonymousUser() throws Exception {
        String requestBody = """
                {
                  "steamApiKey": "%s"
                }
                """.formatted(API_KEY);

        mockMvc.perform(
                put("/api/users/me/steam-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
        then(steamApiKeyService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("스팀API키 형식이 잘못되면 요청을 거부")
    void rejectInvalidFormat() throws Exception {
        MockHttpSession session = createLoginSession();
        String requestBody = """
                {
                  "steamApiKey": "short-key"
                }
                """;

        mockMvc.perform(
                put("/api/users/me/steam-api-key")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
        then(steamApiKeyService).shouldHaveNoInteractions();
    }
    @Test
    @DisplayName("스팀 서버 오류가 발생하면 503을 반환")
    void returnServiceUnavailable() throws Exception {
        MockHttpSession session = createLoginSession();
        given(steamApiKeyService.register(
                STEAM_ID,
                API_KEY
        )).willThrow(new SteamApiException(
                "스팀 서버 연결 오류",
                new RuntimeException()
        ));

        mockMvc.perform(
                        put("/api/users/me/steam-api-key")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "steamApiKey": "%s"
                                    }
                                    """.formatted(API_KEY)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STEAM_API_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "스팀 서비스에 일시적으로 연결할 수 없습니다. 잠시후 다시 시도해 주세요"
                        ));
        then(steamApiKeyService).should().register(STEAM_ID, API_KEY);
    }

    private MockHttpSession createLoginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY,
                STEAM_ID
        );

        return session;
    }
}