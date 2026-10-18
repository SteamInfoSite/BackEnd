package com.steaminfosite.controller;

import com.steaminfosite.dto.request.SteamApiKeyRegisterRequest;
import com.steaminfosite.dto.response.SteamApiKeyStatusResponse;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.service.user.SteamApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/steam-api-key")
public class SteamApiKeyController {
    private final SteamApiKeyService steamApiKeyService;

    @GetMapping
    public ResponseEntity<SteamApiKeyStatusResponse> status(
            @AuthenticationPrincipal Jwt jwt
    ) {
        SteamApiKeyStatus status = steamApiKeyService.getStatus(jwt.getSubject());

        return ResponseEntity.ok(
                SteamApiKeyStatusResponse.from(status)
        );
    }

    @PutMapping
    public ResponseEntity<SteamApiKeyStatusResponse> register(
            @Valid @RequestBody SteamApiKeyRegisterRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        SteamApiKeyStatus status = steamApiKeyService.register(
                jwt.getSubject(),
                request.steamApiKey()
        );

        SteamApiKeyStatusResponse response = SteamApiKeyStatusResponse.from(status);
        if (status == SteamApiKeyStatus.INVALID) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }

        return ResponseEntity.ok(response);
    }
}