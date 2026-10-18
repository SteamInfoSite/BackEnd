package com.steaminfosite.controller;

import com.steaminfosite.dto.response.social.SteamFriendsResponse;
import com.steaminfosite.dto.response.social.SteamProfileResponse;
import com.steaminfosite.service.social.SteamFriendsSearchService;
import com.steaminfosite.service.social.SteamProfileSearchService;
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
@RequestMapping("/api/social")
public class SocialApiController {
    private final SteamProfileSearchService steamProfileSearchService;
    private final SteamFriendsSearchService steamFriendsSearchService;

    @GetMapping("/me")
    public ResponseEntity<SteamProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        SteamProfileResponse response = steamProfileSearchService.getMyProfile(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping("/friends")
    public ResponseEntity<SteamFriendsResponse> getMyFriends(
            @AuthenticationPrincipal Jwt jwt
    ) {
        SteamFriendsResponse response = steamFriendsSearchService.getMyFriends(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}