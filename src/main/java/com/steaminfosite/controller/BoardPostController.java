package com.steaminfosite.controller;

import com.steaminfosite.dto.request.BoardPostWriteRequest;
import com.steaminfosite.dto.response.BoardPageResponse;
import com.steaminfosite.dto.response.BoardPostDetailResponse;
import com.steaminfosite.dto.response.BoardPostListResponse;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.service.board.BoardAuthorNameService;
import com.steaminfosite.service.board.BoardPostLikeService;
import com.steaminfosite.service.board.BoardPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final BoardAuthorNameService boardAuthorNameService;

    @PostMapping
    public ResponseEntity<Void> create(
            @Valid @RequestBody BoardPostWriteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        BoardPostEntity post = boardPostService.create(
                jwt.getSubject(),
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
        Map<String, String> names =
                boardAuthorNameService.getDisplayNames(steamIds);

        Page<BoardPostListResponse> responses = posts.map(post ->
                new BoardPostListResponse(
                        post.getId(),
                        post.getCategory(),
                        post.getTitle(),
                        names.getOrDefault(
                                post.getAuthor().getSteamId(),
                                BoardAuthorNameService.FALLBACK_NAME
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
            @AuthenticationPrincipal Jwt jwt
    ) {
        BoardPostEntity post = boardPostService.detail(postId);
        String authorSteamId = post.getAuthor().getSteamId();
        String authorName = boardAuthorNameService.getDisplayName(authorSteamId);
        String viewerSteamId = jwt == null ? null : jwt.getSubject();
        boolean isMine = authorSteamId.equals(viewerSteamId);

        BoardPostDetailResponse response = new BoardPostDetailResponse(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getContentHtml(),
                authorName,
                isMine,
                boardPostLikeService.likeCount(postId),
                boardPostLikeService.likedBy(postId, viewerSteamId),
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
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardPostService.update(
                postId,
                jwt.getSubject(),
                request.category(),
                request.title(),
                request.contentHtml()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardPostService.delete(postId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}