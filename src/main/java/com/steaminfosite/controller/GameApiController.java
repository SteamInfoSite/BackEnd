package com.steaminfosite.controller;

import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.service.GameIsFreeSearchService;
import com.steaminfosite.service.SteamApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameApiController {
    private final SteamApiService steamApiService;
    private final GameIsFreeSearchService gameIsFreeSearchService;

    @GetMapping("/{appId}")
    public ResponseEntity<GameInfoResponse> getGameInfo(
            @PathVariable long appId
    ){
        return ResponseEntity.ok(steamApiService.getGameInfo(appId));
    }

    @GetMapping
    public ResponseEntity<GameListResponse> getGameList(
            @RequestParam boolean isFree,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        GameListResponse response =
                gameIsFreeSearchService.getGamesByPrice(
                        isFree,
                        page,
                        pageSize
                );

        return ResponseEntity.ok(response);
    }
}
