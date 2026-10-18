package com.steaminfosite.controller;

import com.steaminfosite.dto.response.library.LibraryResponse;
import com.steaminfosite.service.library.LibrarySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library")
public class LibraryApiController {
    private final LibrarySearchService librarySearchService;

    @GetMapping("/me")
    public ResponseEntity<LibraryResponse> getMyLibrary(
            @AuthenticationPrincipal Jwt jwt
    ) {
        LibraryResponse response = librarySearchService.getMyLibrary(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}