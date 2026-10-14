package com.steaminfosite.service.library;

import com.steaminfosite.client.SteamLibraryApiClient;
import com.steaminfosite.dto.api.library.SteamOwnedGame;
import com.steaminfosite.dto.api.library.SteamOwnedGamesData;
import com.steaminfosite.dto.response.library.LibraryGameResponse;
import com.steaminfosite.dto.response.library.LibraryResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamLibraryUnavailableException;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.service.user.SteamApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LibrarySearchServiceImpl implements LibrarySearchService {

    private static final int DB_BATCH_SIZE = 500;

    private final SteamApiKeyService steamApiKeyService;
    private final SteamLibraryApiClient steamLibraryApiClient;
    private final GameRepository gameRepository;

    @Override
    public LibraryResponse getMyLibrary(String steamId) {
        String steamApiKey = steamApiKeyService.getDecryptedApiKey(steamId);

        SteamOwnedGamesData data =
                steamLibraryApiClient.fetchOwnedGames(
                        steamApiKey,
                        steamId
                );

        Integer gameCount = data.getGameCount();
        //정보를 받지 못한 응답을 보유 게임 0개로 처리하지 않음
        if (gameCount == null) {
            throw new SteamLibraryUnavailableException();
        }

        List<SteamOwnedGame> ownedGames = data.getGames();

        if (gameCount < 0) {
            throw invalidOwnedGamesResponse();
        }
        //0개라고 명시된 정상 응답
        if (gameCount == 0) {
            if (ownedGames != null && !ownedGames.isEmpty()) {
                throw invalidOwnedGamesResponse();
            }

            return new LibraryResponse(
                    List.of(),
                    List.of(),
                    Instant.now()
            );
        }
        //게임이 있다고 했는데 목록이 없거나 개수가 다르면 오류 처리
        if (ownedGames == null || ownedGames.size() != gameCount) {
            throw invalidOwnedGamesResponse();
        }

        Map<Long, SteamOwnedGame> ownedByAppId = new LinkedHashMap<>();

        for (SteamOwnedGame owned : ownedGames) {
            if (owned == null
                    || owned.getAppId() == null
                    || owned.getAppId() <= 0) {
                throw invalidOwnedGamesResponse();
            }

            ownedByAppId.putIfAbsent(owned.getAppId(), owned);
        }

        List<Long> appIds = new ArrayList<>(ownedByAppId.keySet());

        Map<Long, GameInfoEntity> storedByAppId = findStoredGames(appIds);

        List<LibraryGameResponse> paidGames = new ArrayList<>();
        List<LibraryGameResponse> unclassifiedGames = new ArrayList<>();

        for (SteamOwnedGame owned : ownedByAppId.values()) {
            GameInfoEntity stored = storedByAppId.get(owned.getAppId());

            if (stored == null) {
                unclassifiedGames.add(toResponse(owned, null));
                continue;
            }

            String type = stored.getType();
            boolean typeKnown = type != null && !type.isBlank();
            boolean isGame = "game".equals(type);
            //게임이 아닌 앱과 무료 게임은 유료 목록에서 제외
            if ((typeKnown && !isGame)
                    || Boolean.TRUE.equals(stored.getIsFree())) {
                continue;
            }

            LibraryGameResponse response = toResponse(owned, stored);

            if (isGame && Boolean.FALSE.equals(stored.getIsFree())) {
                paidGames.add(response);
            } else {
                unclassifiedGames.add(response);
            }
        }

        return new LibraryResponse(
                List.copyOf(paidGames),
                List.copyOf(unclassifiedGames),
                Instant.now()
        );
    }
    //보유한 앱의 DB 정보만 묶어서 조회
    private Map<Long, GameInfoEntity> findStoredGames(List<Long> appIds) {
        Map<Long, GameInfoEntity> result = new HashMap<>();

        for (int start = 0; start < appIds.size(); start += DB_BATCH_SIZE) {
            int end = Math.min(start + DB_BATCH_SIZE, appIds.size());

            List<GameInfoEntity> games =
                    gameRepository.findAllByAppIdIn(
                            appIds.subList(start, end)
                    );

            for (GameInfoEntity game : games) {
                result.put(game.getAppId(), game);
            }
        }

        return result;
    }

    private LibraryGameResponse toResponse(
            SteamOwnedGame owned,
            GameInfoEntity stored
    ) {
        String gameName = owned.getName();

        if (gameName == null || gameName.isBlank()) {
            gameName = stored == null ? null : stored.getGameName();
        }

        if (gameName == null || gameName.isBlank()) {
            gameName = "이름 정보 없음 (App ID: " + owned.getAppId() + ")";
        }

        return new LibraryGameResponse(
                owned.getAppId(),
                gameName,
                stored == null ? null : stored.getHeaderImage(),
                owned.getIconHash(),
                owned.getPlaytimeForever(),
                owned.getPlaytimeTwoWeeks(),
                owned.getLastPlayed(),
                stored != null && "game".equals(stored.getType())
        );
    }

    private SteamApiException invalidOwnedGamesResponse() {
        return new SteamApiException(
                "스팀 보유 게임 응답의 개수 또는 게임 정보가 올바르지 않습니다",
                null
        );
    }
}