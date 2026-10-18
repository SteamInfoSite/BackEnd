package com.steaminfosite.controller;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.JwtAccessTokenService;
import com.steaminfosite.security.SteamApiKeyEncryptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SteamApiKeyIntegrationTest {

    private static final String STEAM_ID = "76561198000000000";
    private static final String API_KEY = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository siteUserRepository;

    @Autowired
    private SteamApiKeyEncryptor steamApiKeyEncryptor;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SteamUserApiClient steamUserApiClient;

    @Test
    @DisplayName("JWT로 API 키 등록 후 암호화 저장과 상태 조회")
    void registerAndGetStatus() throws Exception {
        siteUserRepository.saveAndFlush(new SiteUserEntity(STEAM_ID));
        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        given(steamUserApiClient.validateApiKey(API_KEY, STEAM_ID))
                .willReturn(true);

        mockMvc.perform(
                        put("/api/users/me/steam-api-key")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "steamApiKey": "%s"
                                }
                                """.formatted(API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.usable").value(true));

        siteUserRepository.flush();
        entityManager.clear();

        SiteUserEntity savedUser = siteUserRepository
                .findBySteamId(STEAM_ID)
                .orElseThrow();

        assertThat(savedUser.getSteamApiKeyStatus())
                .isEqualTo(SteamApiKeyStatus.VALID);
        assertThat(savedUser.getEncryptedSteamApiKey())
                .isNotBlank()
                .isNotEqualTo(API_KEY);
        assertThat(steamApiKeyEncryptor.decrypt(
                savedUser.getEncryptedSteamApiKey()
        )).isEqualTo(API_KEY);

        mockMvc.perform(
                        get("/api/users/me/steam-api-key")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.usable").value(true));
    }

    @Test
    @DisplayName("JWT가 없으면 API 키 조회와 등록 모두 거부")
    void rejectWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me/steam-api-key"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/users/me/steam-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamApiKey": "%s"
                                }
                                """.formatted(API_KEY)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT가 있어도 API 키 형식이 잘못되면 거부")
    void rejectInvalidFormat() throws Exception {
        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        mockMvc.perform(put("/api/users/me/steam-api-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamApiKey": "short-key"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}