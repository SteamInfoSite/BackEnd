package com.steaminfosite.controller;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.dto.response.SteamDisplayNameResponse;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.service.board.BoardCurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
    public SteamDisplayNameResponse displayName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String sessionSteamId = session == null
                ? null
                : (String) session.getAttribute(
                SteamAuthController.STEAM_ID_SESSION_KEY
        );

        SiteUserEntity user = currentUser.requireUser(sessionSteamId);
        String name = steamProfileClient.getDisplayName(user.getSteamId());

        return new SteamDisplayNameResponse(name);
    }
}