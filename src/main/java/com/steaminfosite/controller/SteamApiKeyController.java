package com.steaminfosite.controller;

import com.steaminfosite.dto.request.SteamApiKeyRegisterRequest;
import com.steaminfosite.dto.response.SteamApiKeyStatusResponse;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.service.user.SteamApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/steam-api-key")
public class SteamApiKeyController {
    private final SteamApiKeyService steamApiKeyService;

    @PutMapping
    public ResponseEntity<SteamApiKeyStatusResponse> register(
            @Valid @RequestBody
            SteamApiKeyRegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        String steamId = requireSteamId(servletRequest);
        SteamApiKeyStatus status = steamApiKeyService.register(
                steamId,
                request.steamApiKey()
        );
        SteamApiKeyStatusResponse response = SteamApiKeyStatusResponse.from(status);
        if (status == SteamApiKeyStatus.INVALID) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }

        return ResponseEntity.ok(response);
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
        if (!(steamIdValue instanceof String steamId) || steamId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다"
            );
        }

        return steamId;
    }
}