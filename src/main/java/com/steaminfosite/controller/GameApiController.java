package com.steaminfosite.controller;

import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.service.game.*;
import com.steaminfosite.service.SteamApiService;
import com.steaminfosite.service.SteamGenreListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameApiController {
    private final SteamApiService steamApiService;
    private final GameIsFreeSearchService gameIsFreeSearchService;
    private final SteamGenreListService steamGenreListService;
    private final GameGenreSearchService gameGenreSearchService;
    private final GameHotSearchService gameHotSearchService;
    private final GameDiscountSearchService gameDiscountSearchService;
    private final GameNewSearchService gameNewSearchService;

    @GetMapping("/{appId}")
    public ResponseEntity<GameInfoResponse> getGameInfo(
            @PathVariable long appId
    ){
        return ResponseEntity.ok(steamApiService.getGameInfo(appId));
    }

    @GetMapping
    public ResponseEntity<GameListResponse> getGameList(
            @RequestParam boolean isFree,
            @RequestParam(defaultValue = "all") String genre,
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "app_id_asc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        GameListResponse response = gameIsFreeSearchService.getGamesByPrice(
                isFree,
                genre,
                platform,
                sort,
                page,
                pageSize
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getGameGenres() {
        return ResponseEntity.ok(
                steamGenreListService.getGameGenreNames()
        );
    }

    @GetMapping("/by-genre")
    public ResponseEntity<GameListResponse> getGamesByGenre(
            @RequestParam(defaultValue = "all") String genre,
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "app_id_asc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        GameListResponse response = gameGenreSearchService.getGamesByGenre(
                genre,
                platform,
                sort,
                page,
                pageSize
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/hot")
    public ResponseEntity<GameListResponse> getHotGames(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(
                gameHotSearchService.getHotGames(
                        platform,
                        page,
                        pageSize
                )
        );
    }

    @GetMapping("/discount")
    public ResponseEntity<GameListResponse> getDiscountGames(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "discount_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(
                gameDiscountSearchService.getDiscountGames(
                        platform,
                        sort,
                        page,
                        pageSize
                )
        );
    }

    @GetMapping("/new")
    public ResponseEntity<GameListResponse> getNewGames(
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "released_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(
                gameNewSearchService.getNewGames(
                        platform,
                        sort,
                        page,
                        pageSize
                )
        );
    }
}
