package com.steaminfosite.controller;

import com.steaminfosite.dto.response.BoardPostLikeResponse;
import com.steaminfosite.service.board.BoardPostLikeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            HttpServletRequest servletRequest
    ) {
        HttpSession session = servletRequest.getSession(false);
        String steamId = session == null
                ? null
                : (String) session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY);

        return ResponseEntity.ok(boardPostLikeService.like(postId, steamId));
    }
}
