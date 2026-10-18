package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.dto.response.SteamDisplayNameResponse;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.service.board.BoardCurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/me")
public class BoardProfileController {
    private final BoardCurrentUserService currentUser;
    private final SteamProfileClient steamProfileClient;

    @GetMapping("/display-name")
    public SteamDisplayNameResponse displayName(
            @AuthenticationPrincipal Jwt jwt
    ) {
        SiteUserEntity user = currentUser.requireUser(jwt.getSubject());
        String name = steamProfileClient.getDisplayName(user.getSteamId());
        return new SteamDisplayNameResponse(name);
    }
}