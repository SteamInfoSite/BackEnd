package com.steaminfosite.controller;

import com.steaminfosite.dto.response.library.LibraryResponse;
import com.steaminfosite.service.library.LibrarySearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library")
public class LibraryApiController {
    private final LibrarySearchService librarySearchService;

    @GetMapping("/me")
    public ResponseEntity<LibraryResponse> getMyLibrary(
            HttpServletRequest request
    ) {
        String steamId = requireSteamId(request);

        LibraryResponse response = librarySearchService.getMyLibrary(steamId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    private String requireSteamId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다"
            );
        }

        Object steamIdValue = session.getAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY
        );

        if (!(steamIdValue instanceof String steamId)
                || steamId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다"
            );
        }

        return steamId;
    }
}