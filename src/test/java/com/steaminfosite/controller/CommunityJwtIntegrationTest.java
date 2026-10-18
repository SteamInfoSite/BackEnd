package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardPostRepository;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.JwtAccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityJwtIntegrationTest {

    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private SiteUserRepository users;

    @Autowired
    private BoardPostRepository posts;

    @MockitoBean
    private SteamProfileClient steamProfileClient;

    private Long postId;

    @BeforeEach
    void setUp() {
        SiteUserEntity author = users.save(new SiteUserEntity(STEAM_ID));

        postId = posts.saveAndFlush(BoardPostEntity.create(
                author,
                BoardCategory.CHAT,
                "테스트 글",
                "<p>본문</p>",
                "본문"
        )).getId();
    }

    @Test
    void getPostsWithoutJwt() throws Exception {
        given(steamProfileClient.getDisplayNames(anySet()))
                .willReturn(Map.of());

        mockMvc.perform(get("/api/community/posts"))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getRequest().getSession(false))
                                .isNull());
    }

    @Test
    void rejectLikeWithoutValidJwt() throws Exception {
        mockMvc.perform(
                        post("/api/community/posts/{postId}/likes", postId)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/community/posts/{postId}/likes", postId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likeWithJwtWithoutSessionOrCsrf() throws Exception {
        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        mockMvc.perform(
                        post("/api/community/posts/{postId}/likes", postId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(result ->
                        assertThat(result.getRequest().getSession(false))
                                .isNull());
    }

    @Test
    void getDisplayNameRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/community/me/display-name"))
                .andExpect(status().isUnauthorized());

        given(steamProfileClient.getDisplayName(STEAM_ID))
                .willReturn("테스트 사용자");

        String token = jwtAccessTokenService.issueAccessToken(STEAM_ID);

        mockMvc.perform(
                        get("/api/community/me/display-name")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName")
                        .value("테스트 사용자"));
    }
}