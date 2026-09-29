package com.steaminfosite.controller;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.SteamApiKeyEncryptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
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
class SteamApiKeyIntegrationTest {  //H2에서 컨트롤러 - 서비스 - 암호화 - JPA저장을 확인하는 통합 테스트
    private static final String STEAM_ID = "76561198000000000";
    private static final String API_KEY = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository siteUserRepository;

    @Autowired
    private SteamApiKeyEncryptor steamApiKeyEncryptor;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SteamUserApiClient steamUserApiClient;

    @Test
    @DisplayName("API키 등록 후 암호화된 값을 H2 DB에 저장하고 상태를 조회")
    void registerAndGetStatus() throws Exception {
        siteUserRepository.saveAndFlush(new SiteUserEntity(STEAM_ID));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY,
                STEAM_ID
        );
        given(steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        )).willReturn(true);

        mockMvc.perform(
                put("/api/users/me/steam-api-key")
                        .session(session)
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                 {
                                 "steamApiKey": "%s"
                                 }
                                 """.formatted(API_KEY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.usable").value(true));

        //엔티티 내용을 DB에 반영한 뒤 다시 조회
        siteUserRepository.flush();
        entityManager.clear();
        SiteUserEntity savedUser = siteUserRepository.findBySteamId(STEAM_ID).orElseThrow();

        assertThat(savedUser.getSteamApiKeyStatus()).isEqualTo(SteamApiKeyStatus.VALID);
        assertThat(savedUser.getEncryptedSteamApiKey()).isNotBlank().isNotEqualTo(API_KEY);
        assertThat(steamApiKeyEncryptor.decrypt(
                savedUser.getEncryptedSteamApiKey()
        )).isEqualTo(API_KEY);
        mockMvc.perform(
                get("/api/users/me/steam-api-key")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.usable").value(true));
    }
}