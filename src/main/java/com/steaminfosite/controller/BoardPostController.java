package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.dto.request.BoardPostWriteRequest;
import com.steaminfosite.dto.response.BoardPageResponse;
import com.steaminfosite.dto.response.BoardPostDetailResponse;
import com.steaminfosite.dto.response.BoardPostListResponse;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.service.board.BoardPostService;
import com.steaminfosite.service.board.BoardPostLikeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class BoardPostController {

    private final BoardPostService boardPostService;
    private final BoardPostLikeService boardPostLikeService;
    private final SteamProfileClient steamProfileClient;

    @PostMapping
    public ResponseEntity<Void> create(
            @Valid @RequestBody BoardPostWriteRequest request,
            HttpServletRequest servletRequest
    ) {
        BoardPostEntity post = boardPostService.create(
                sessionSteamId(servletRequest),
                request.category(),
                request.title(),
                request.contentHtml()
        );

        return ResponseEntity
                .created(URI.create("/api/community/posts/" + post.getId()))
                .build();
    }

    @GetMapping
    public ResponseEntity<BoardPageResponse<BoardPostListResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BoardPostEntity> posts = boardPostService.list(page, size);

        List<Long> postIds = posts.getContent().stream()
                .map(BoardPostEntity::getId)
                .toList();
        Map<Long, Long> commentCounts = boardPostService.commentCounts(postIds);
        Map<Long, Long> likeCounts = boardPostLikeService.likeCounts(postIds);

        Set<String> steamIds = new LinkedHashSet<>();
        for (BoardPostEntity post : posts.getContent()) {
            steamIds.add(post.getAuthor().getSteamId());
        }
        Map<String, String> names = steamProfileClient.getDisplayNames(steamIds);

        Page<BoardPostListResponse> responses = posts.map(post ->
                new BoardPostListResponse(
                        post.getId(),
                        post.getCategory(),
                        post.getTitle(),
                        names.getOrDefault(
                                post.getAuthor().getSteamId(), "Steam 사용자"
                        ),
                        post.getViewCount(),
                        commentCounts.getOrDefault(post.getId(), 0L),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        post.getCreatedAt()
                )
        );

        return ResponseEntity.ok(BoardPageResponse.from(responses));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<BoardPostDetailResponse> detail(
            @PathVariable Long postId,
            HttpServletRequest servletRequest
    ) {
        BoardPostEntity post = boardPostService.detail(postId);
        String authorSteamId = post.getAuthor().getSteamId();
        String authorName = steamProfileClient.getDisplayName(
                authorSteamId
        );
        boolean isMine = authorSteamId.equals(sessionSteamId(servletRequest));

        BoardPostDetailResponse response = new BoardPostDetailResponse(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getContentHtml(),
                authorName,
                isMine,
                boardPostLikeService.likeCount(postId),
                boardPostLikeService.likedBy(postId, sessionSteamId(servletRequest)),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<Void> update(
            @PathVariable Long postId,
            @Valid @RequestBody BoardPostWriteRequest request,
            HttpServletRequest servletRequest
    ) {
        boardPostService.update(
                postId,
                sessionSteamId(servletRequest),
                request.category(),
                request.title(),
                request.contentHtml()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            HttpServletRequest servletRequest
    ) {
        boardPostService.delete(postId, sessionSteamId(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private String sessionSteamId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? null
                : (String) session.getAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY
        );
    }
}
