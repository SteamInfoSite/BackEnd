package com.steaminfosite.controller;

import com.steaminfosite.dto.request.BoardCommentWriteRequest;
import com.steaminfosite.service.board.BoardCommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class BoardCommentController {

    private final BoardCommentService boardCommentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Void> create(
            @PathVariable Long postId,
            @Valid @RequestBody BoardCommentWriteRequest request,
            HttpServletRequest servletRequest
    ) {
        boardCommentService.create(
                postId,
                sessionSteamId(servletRequest),
                request.content()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @Valid @RequestBody BoardCommentWriteRequest request,
            HttpServletRequest servletRequest
    ) {
        boardCommentService.update(
                commentId,
                sessionSteamId(servletRequest),
                request.content()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            HttpServletRequest servletRequest
    ) {
        boardCommentService.delete(commentId, sessionSteamId(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private String sessionSteamId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? null
                : (String) session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY);
    }
}