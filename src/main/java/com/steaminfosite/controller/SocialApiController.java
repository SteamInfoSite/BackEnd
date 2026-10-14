package com.steaminfosite.controller;

import com.steaminfosite.dto.response.social.SteamFriendsResponse;
import com.steaminfosite.dto.response.social.SteamProfileResponse;
import com.steaminfosite.service.social.SteamFriendsSearchService;
import com.steaminfosite.service.social.SteamProfileSearchService;
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
@RequestMapping("/api/social")
public class SocialApiController {
    private final SteamProfileSearchService steamProfileSearchService;
    private final SteamFriendsSearchService steamFriendsSearchService;

    @GetMapping("/me")
    public ResponseEntity<SteamProfileResponse> getMyProfile(
            HttpServletRequest request
    ) {
        String steamId = requireSteamId(request);

        SteamProfileResponse response =
                steamProfileSearchService.getMyProfile(steamId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping("/friends")
    public ResponseEntity<SteamFriendsResponse> getMyFriends(
            HttpServletRequest request
    ) {
        String steamId = requireSteamId(request);

        SteamFriendsResponse response = steamFriendsSearchService.getMyFriends(steamId);

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