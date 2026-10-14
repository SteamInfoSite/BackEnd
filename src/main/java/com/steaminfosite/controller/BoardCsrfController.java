package com.steaminfosite.controller;

import com.steaminfosite.dto.response.BoardCsrfResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/community")
public class BoardCsrfController {

    @GetMapping("/csrf")
    public ResponseEntity<BoardCsrfResponse> csrfToken(
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        HttpSession session = request.getSession(false);
        Object steamId = session == null
                ? null
                : session.getAttribute(SteamAuthController.STEAM_ID_SESSION_KEY);
        if (!(steamId instanceof String id) || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new BoardCsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }
}
