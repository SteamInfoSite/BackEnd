package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SteamAuthIntegrationTest {    //H2에 사용자까지 저장되는지 확인하는 통합 테스트

    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository siteUserRepository;

    @MockitoBean
    private SteamOpenIdClient steamOpenIdClient;

    @Test
    @DisplayName("스팀 로그인 성공 시 사용자를 H2 DB와 세션에 저장")
    void login() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY,
                "test-state"
        );
        given(steamOpenIdClient.verifyCallback(any())).willReturn(true);
        given(steamOpenIdClient.extractSteamId(any())).willReturn(STEAM_ID);

        mockMvc.perform(get("/api/auth/steam/callback")
                        .session(session)
                        .param("state", "test-state")
                        .param("openid.mode", "id_res")
                        .param(
                                "openid.claimed_id",
                                "https://steamcommunity.com/openid/id/"
                                        + STEAM_ID
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "스팀OpenID 인증에 성공했습니다. "
                                + "스팀64 ID: "
                                + STEAM_ID
                ));

        assertThat(session.getAttribute(SteamAuthController.STEAM_LOGIN_STATE_SESSION_KEY)).isNull();
        assertThat(session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY)).isEqualTo(STEAM_ID);
        Optional<SiteUserEntity> savedUser = siteUserRepository.findBySteamId(STEAM_ID);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getSteamId()).isEqualTo(STEAM_ID);
        assertThat(savedUser.get().getRole()).isEqualTo(SiteUserRole.USER);
    }
}