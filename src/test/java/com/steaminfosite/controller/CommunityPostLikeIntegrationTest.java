package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.board.BoardPostLikeEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardPostLikeRepository;
import com.steaminfosite.repository.BoardPostRepository;
import com.steaminfosite.repository.SiteUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityPostLikeIntegrationTest {
    private static final String AUTHOR_STEAM_ID = "76561198000000000";
    private static final String OTHER_STEAM_ID = "76561198000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository users;

    @Autowired
    private BoardPostRepository posts;

    @Autowired
    private BoardPostLikeRepository likes;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SteamProfileClient steamProfileClient;

    private Long postId;

    @BeforeEach
    void setUp() {
        SiteUserEntity author = users.save(new SiteUserEntity(AUTHOR_STEAM_ID));
        users.save(new SiteUserEntity(OTHER_STEAM_ID));
        postId = posts.saveAndFlush(BoardPostEntity.create(
                author, BoardCategory.CHAT, "테스트 글", "<p>본문</p>", "본문"
        )).getId();
    }

    @Test
    void givenLoggedInUser_whenLike_thenCountAndMyStatusAreReturned() throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session(AUTHOR_STEAM_ID)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        given(steamProfileClient.getDisplayName(AUTHOR_STEAM_ID)).willReturn("작성자");
        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .session(session(AUTHOR_STEAM_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .session(session(OTHER_STEAM_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));

        given(steamProfileClient.getDisplayNames(anySet()))
                .willReturn(Map.of(AUTHOR_STEAM_ID, "작성자"));
        mockMvc.perform(get("/api/community/posts?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].likeCount").value(1));
    }

    @Test
    void givenAlreadyLikedPost_whenLikeAgain_thenConflictAndCountStaysOne() throws Exception {
        MockHttpSession session = session(AUTHOR_STEAM_ID);
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(likes.countByPost_Id(postId)).isEqualTo(1);
    }

    @Test
    void givenDuplicateRows_whenPersisted_thenDatabaseRejectsSecondLike() {
        SiteUserEntity user = users.findBySteamId(AUTHOR_STEAM_ID).orElseThrow();
        BoardPostEntity post = posts.findById(postId).orElseThrow();
        likes.saveAndFlush(BoardPostLikeEntity.create(post, user));

        assertThatThrownBy(() -> likes.saveAndFlush(BoardPostLikeEntity.create(post, user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenTwoUsers_whenEachLikes_thenCountIsTwo() throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session(AUTHOR_STEAM_ID)).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session(OTHER_STEAM_ID)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(2));
    }

    @Test
    void givenNoLogin_whenLike_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId))
                .andExpect(status().isUnauthorized());
        assertThat(likes.countByPost_Id(postId)).isZero();
    }

    @Test
    void givenMissingPost_whenLike_thenNotFound() throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId + 1)
                        .session(session(AUTHOR_STEAM_ID)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenLikedPost_whenAuthorDeletes_thenLikesAreRemoved() throws Exception {
        mockMvc.perform(post("/api/community/posts/{postId}/likes", postId)
                        .session(session(OTHER_STEAM_ID)).with(csrf()))
                .andExpect(status().isOk());
        entityManager.clear();

        mockMvc.perform(delete("/api/community/posts/{postId}", postId)
                        .session(session(AUTHOR_STEAM_ID)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(likes.countByPost_Id(postId)).isZero();
        assertThat(posts.existsById(postId)).isFalse();
    }

    private static MockHttpSession session(String steamId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SteamAuthController.STEAM_ID_SESSION_KEY, steamId);
        return session;
    }
}
