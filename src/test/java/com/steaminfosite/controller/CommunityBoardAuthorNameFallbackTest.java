package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.entity.board.BoardCommentEntity;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.repository.BoardCommentRepository;
import com.steaminfosite.repository.BoardPostRepository;
import com.steaminfosite.repository.SiteUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityBoardAuthorNameFallbackTest {
    private static final String AUTHOR_STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository users;

    @Autowired
    private BoardPostRepository posts;

    @Autowired
    private BoardCommentRepository comments;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SteamProfileClient steamProfileClient;

    private Long postId;

    @BeforeEach
    void setUp() {
        SiteUserEntity author =
                users.save(new SiteUserEntity(AUTHOR_STEAM_ID));

        BoardPostEntity post = posts.saveAndFlush(
                BoardPostEntity.create(
                        author,
                        BoardCategory.CHAT,
                        "테스트 글",
                        "<p>본문</p>",
                        "본문"
                )
        );
        postId = post.getId();

        comments.saveAndFlush(
                BoardCommentEntity.create(post, author, "테스트 댓글")
        );

        entityManager.clear();
    }

    @Test
    void givenSteamFailure_whenListingPosts_thenFallbackNameIsReturned()
            throws Exception {
        given(steamProfileClient.getDisplayNames(anySet()))
                .willThrow(steamFailure());

        mockMvc.perform(get("/api/community/posts")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorName")
                        .value("Steam 사용자"));
    }

    @Test
    void givenSteamFailure_whenReadingPost_thenFallbackNameIsReturned()
            throws Exception {
        given(steamProfileClient.getDisplayName(AUTHOR_STEAM_ID))
                .willThrow(steamFailure());

        mockMvc.perform(get("/api/community/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName")
                        .value("Steam 사용자"))
                .andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    void givenSteamFailure_whenListingComments_thenFallbackNameIsReturned()
            throws Exception {
        given(steamProfileClient.getDisplayNames(anySet()))
                .willThrow(steamFailure());

        mockMvc.perform(get(
                        "/api/community/posts/{postId}/comments",
                        postId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorName")
                        .value("Steam 사용자"));
    }

    private static SteamApiException steamFailure() {
        return new SteamApiException(
                "Steam 조회 실패",
                new IllegalStateException("테스트용 오류")
        );
    }
}