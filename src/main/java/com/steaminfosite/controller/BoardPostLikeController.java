package com.steaminfosite.controller;

import com.steaminfosite.dto.response.BoardPostLikeResponse;
import com.steaminfosite.service.board.BoardPostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts/{postId}/likes")
public class BoardPostLikeController {

    private final BoardPostLikeService boardPostLikeService;

    @PostMapping
    public ResponseEntity<BoardPostLikeResponse> like(
            @PathVariable Long postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                boardPostLikeService.like(postId, jwt.getSubject())
        );
    }
}