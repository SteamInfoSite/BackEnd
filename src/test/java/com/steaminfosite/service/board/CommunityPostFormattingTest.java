package com.steaminfosite.service.board;

import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardCommentRepository;
import com.steaminfosite.repository.BoardPostLikeRepository;
import com.steaminfosite.repository.BoardPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommunityPostFormattingTest {

    @Mock
    private BoardPostRepository posts;

    @Mock
    private BoardCommentRepository comments;

    @Mock
    private BoardPostLikeRepository likes;

    @Mock
    private BoardCurrentUserService currentUser;

    @InjectMocks
    private BoardPostService service;

    @Test
    void givenAllowedAndDisallowedColors_whenCreatingPost_thenOnlyPaletteColorsRemain() {
        String steamId = "76561198000000000";
        given(currentUser.requireUser(steamId)).willReturn(new SiteUserEntity(steamId));
        given(posts.save(any(BoardPostEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        BoardPostEntity post = service.create(
                steamId,
                BoardCategory.CHAT,
                "색상 테스트",
                "<p><span style=\"font-size: 18px; color: rgb(220, 38, 38); "
                        + "background-image: url(javascript:alert(1))\">빨강</span> "
                        + "<span style=\"color: #2563eb\">파랑</span> "
                        + "<span style=\"color: #123456\">기본</span></p>"
        );

        assertThat(post.getContentHtml())
                .contains("font-size: 18px", "color: #dc2626", "color: #2563eb")
                .doesNotContain("background-image", "javascript:", "#123456");
        assertThat(post.getContentText()).isEqualTo("빨강 파랑 기본");
    }
}
