package com.steaminfosite.controller;

import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.service.SteamApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameApiController {
    private final SteamApiService steamApiService;

    @GetMapping("/{appId}")
    public ResponseEntity<GameInfoResponse> getGameInfo(
            @PathVariable long appId
    ){
        return ResponseEntity.ok(steamApiService.getGameInfo(appId));
    }
}
