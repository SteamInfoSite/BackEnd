package com.steaminfosite.controller;

import com.steaminfosite.security.JwtAccessTokenService;
import com.steaminfosite.service.social.SteamFriendsSearchService;
import com.steaminfosite.service.social.SteamProfileSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialApiControllerJwtTest {
    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @MockitoBean
    private SteamProfileSearchService steamProfileSearchService;

    @MockitoBean
    private SteamFriendsSearchService steamFriendsSearchService;

    @Test
    @DisplayName("JWT의 Steam ID로 내 프로필 조회")
    void getProfileWithJwt() throws Exception {
        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        mockMvc.perform(get("/api/social/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getRequest().getSession(false)).isNull());

        then(steamProfileSearchService).should().getMyProfile(STEAM_ID);
    }

    @Test
    @DisplayName("JWT의 Steam ID로 친구 목록 조회")
    void getFriendsWithJwt() throws Exception {
        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        mockMvc.perform(get("/api/social/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getRequest().getSession(false)).isNull());

        then(steamFriendsSearchService).should().getMyFriends(STEAM_ID);
    }

    @Test
    @DisplayName("JWT가 없으면 Social 조회 거부")
    void rejectWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/social/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/social/friends"))
                .andExpect(status().isUnauthorized());

        then(steamProfileSearchService).shouldHaveNoInteractions();
        then(steamFriendsSearchService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잘못된 JWT로 Social 조회 거부")
    void rejectInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/social/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        then(steamProfileSearchService).shouldHaveNoInteractions();
    }
}