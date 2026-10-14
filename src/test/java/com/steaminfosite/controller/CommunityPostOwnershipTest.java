package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.service.board.BoardPostService;
import com.steaminfosite.service.board.BoardAuthorNameService;
import com.steaminfosite.service.board.BoardPostLikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class CommunityPostOwnershipTest {
    private static final String AUTHOR_STEAM_ID = "76561198000000000";
    private static final String OTHER_STEAM_ID = "76561198000000001";

    @Mock
    private BoardPostService boardPostService;

    @Mock
    private BoardPostLikeService boardPostLikeService;

    @Mock
    private SteamProfileClient steamProfileClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new BoardPostController(
                boardPostService,
                boardPostLikeService,
                new BoardAuthorNameService(steamProfileClient)
        )).build();

        BoardPostEntity post = BoardPostEntity.create(
                new SiteUserEntity(AUTHOR_STEAM_ID),
                BoardCategory.CHAT,
                "테스트 글",
                "<p>본문</p>",
                "본문"
        );
        given(boardPostService.detail(1L)).willReturn(post);
        given(steamProfileClient.getDisplayName(AUTHOR_STEAM_ID))
                .willReturn("작성자");
    }

    @Test
    void givenAuthorSession_whenDetail_thenIsMineIsTrue() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SteamAuthController.STEAM_ID_SESSION_KEY, AUTHOR_STEAM_ID);

        mockMvc.perform(get("/api/community/posts/1").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.isMine").value(true));
    }

    @Test
    void givenOtherUserSession_whenDetail_thenIsMineIsFalse() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SteamAuthController.STEAM_ID_SESSION_KEY, OTHER_STEAM_ID);

        mockMvc.perform(get("/api/community/posts/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMine").value(false));
    }

    @Test
    void givenNoSession_whenDetail_thenIsMineIsFalse() throws Exception {
        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMine").value(false));
    }
}
