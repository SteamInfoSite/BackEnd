package com.steaminfosite.controller;

import com.steaminfosite.dto.request.BoardPostWriteRequest;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.service.board.BoardPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class BoardPostController {

    private final BoardPostService boardPostService;

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
                : (String) session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY);
    }
}