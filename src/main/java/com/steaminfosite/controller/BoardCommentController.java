package com.steaminfosite.controller;

import com.steaminfosite.dto.request.BoardCommentWriteRequest;
import com.steaminfosite.dto.response.BoardCommentResponse;
import com.steaminfosite.dto.response.BoardPageResponse;
import com.steaminfosite.entity.board.BoardCommentEntity;
import com.steaminfosite.service.board.BoardAuthorNameService;
import com.steaminfosite.service.board.BoardCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;
    private final BoardAuthorNameService boardAuthorNameService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Void> create(
            @PathVariable Long postId,
            @Valid @RequestBody BoardCommentWriteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardCommentService.create(
                postId,
                jwt.getSubject(),
                request.content()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<BoardPageResponse<BoardCommentResponse>> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BoardCommentEntity> comments =
                boardCommentService.list(postId, page, size);

        Set<String> steamIds = new LinkedHashSet<>();
        for (BoardCommentEntity comment : comments.getContent()) {
            steamIds.add(comment.getAuthor().getSteamId());
        }
        Map<String, String> names =
                boardAuthorNameService.getDisplayNames(steamIds);

        Page<BoardCommentResponse> responses = comments.map(comment ->
                new BoardCommentResponse(
                        comment.getId(),
                        postId,
                        names.getOrDefault(
                                comment.getAuthor().getSteamId(),
                                BoardAuthorNameService.FALLBACK_NAME
                        ),
                        comment.getContent(),
                        comment.getCreatedAt(),
                        comment.getUpdatedAt()
                )
        );

        return ResponseEntity.ok(BoardPageResponse.from(responses));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @Valid @RequestBody BoardCommentWriteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardCommentService.update(
                commentId,
                jwt.getSubject(),
                request.content()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardCommentService.delete(commentId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}