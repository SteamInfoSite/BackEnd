package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.dto.response.BoardPostDetailResponse;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.service.board.BoardAuthorNameService;
import com.steaminfosite.service.board.BoardPostLikeService;
import com.steaminfosite.service.board.BoardPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

    private BoardPostController controller;

    @BeforeEach
    void setUp() {
        controller = new BoardPostController(
                boardPostService,
                boardPostLikeService,
                new BoardAuthorNameService(steamProfileClient)
        );

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
    void detailMarksAuthorAsOwner() {
        ResponseEntity<BoardPostDetailResponse> response =
                controller.detail(1L, jwt(AUTHOR_STEAM_ID));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isMine()).isTrue();
        assertThat(response.getHeaders().getCacheControl())
                .isEqualTo("no-store");

        then(boardPostLikeService).should()
                .likedBy(1L, AUTHOR_STEAM_ID);
    }

    @Test
    void detailDoesNotMarkOtherUserAsOwner() {
        ResponseEntity<BoardPostDetailResponse> response =
                controller.detail(1L, jwt(OTHER_STEAM_ID));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isMine()).isFalse();

        then(boardPostLikeService).should()
                .likedBy(1L, OTHER_STEAM_ID);
    }

    @Test
    void detailDoesNotMarkAnonymousViewerAsOwner() {
        ResponseEntity<BoardPostDetailResponse> response =
                controller.detail(1L, null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isMine()).isFalse();

        then(boardPostLikeService).should()
                .likedBy(1L, null);
    }

    private Jwt jwt(String steamId) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(steamId)
                .build();
    }
}