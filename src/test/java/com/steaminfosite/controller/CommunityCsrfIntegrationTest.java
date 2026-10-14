package com.steaminfosite.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardPostRepository;
import com.steaminfosite.repository.SiteUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityCsrfIntegrationTest {
    private static final String AUTHOR_STEAM_ID = "76561198000000000";
    private static final String OTHER_STEAM_ID = "76561198000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SiteUserRepository users;

    @Autowired
    private BoardPostRepository posts;

    private Long postId;

    @BeforeEach
    void setUp() {
        SiteUserEntity author = users.save(new SiteUserEntity(AUTHOR_STEAM_ID));
        postId = posts.saveAndFlush(BoardPostEntity.create(
                author, BoardCategory.CHAT, "테스트 글", "<p>본문</p>", "본문"
        )).getId();
    }

    @Test
    void givenNoLogin_whenRequestingToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/community/csrf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenLogin_whenRequestingToken_thenTokenIsReturnedWithoutCaching() throws Exception {
        mockMvc.perform(get("/api/community/csrf").session(session(AUTHOR_STEAM_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void givenLoginWithoutToken_whenChangingCommunity_thenForbidden() throws Exception {
        MockHttpSession session = session(AUTHOR_STEAM_ID);
        MockHttpServletRequestBuilder[] requests = {
                post("/api/community/posts"),
                patch("/api/community/posts/{postId}", postId),
                delete("/api/community/posts/{postId}", postId),
                post("/api/community/posts/{postId}/comments", postId),
                patch("/api/community/comments/1"),
                delete("/api/community/comments/1"),
                post("/api/community/posts/{postId}/likes", postId)
        };

        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request.session(session))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void givenTokenFromSameSession_whenLikingPost_thenRequestSucceeds() throws Exception {
        MockHttpSession session = session(AUTHOR_STEAM_ID);
        String token = tokenFor(session);

        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session)
                        .header("X-CSRF-TOKEN", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void givenTokenFromAnotherSession_whenLikingPost_thenForbidden() throws Exception {
        String token = tokenFor(session(AUTHOR_STEAM_ID));

        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session(OTHER_STEAM_ID))
                        .header("X-CSRF-TOKEN", token))
                .andExpect(status().isForbidden());
    }

    private String tokenFor(MockHttpSession session) throws Exception {
        String body = mockMvc.perform(get("/api/community/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        return response.get("token").asText();
    }

    private static MockHttpSession session(String steamId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SteamAuthController.STEAM_ID_SESSION_KEY, steamId);
        return session;
    }
}
