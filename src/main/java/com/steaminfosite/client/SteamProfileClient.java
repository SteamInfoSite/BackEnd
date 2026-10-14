package com.steaminfosite.client;

import com.steaminfosite.exception.SteamApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SteamProfileClient {
    private static final int MAX_STEAM_IDS = 100;

    private final RestClient steamWebApiRestClient;
    private final String steamApiKey;

    public SteamProfileClient(
            @Qualifier("steamWebApiRestClient") RestClient steamWebApiRestClient,
            @Value("${steam.api.key:}") String steamApiKey
    ) {
        this.steamWebApiRestClient = steamWebApiRestClient;
        this.steamApiKey = steamApiKey;
    }

    public String getDisplayName(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException("Steam64 ID가 필요합니다");
        }

        String name = getDisplayNames(List.of(steamId)).get(steamId);
        if (name == null) {
            throw new SteamApiException(
                    "스팀 프로필 응답에 표시 이름이 없습니다",
                    new IllegalStateException("Steam player summary missing")
            );
        }
        return name;
    }

    public Map<String, String> getDisplayNames(Collection<String> steamIds) {
        if (steamIds == null || steamIds.stream()
                .anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("유효한 Steam64 ID가 필요합니다");
        }

        Set<String> uniqueIds = new LinkedHashSet<>(steamIds);
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        if (uniqueIds.size() > MAX_STEAM_IDS) {
            throw new IllegalArgumentException(
                    "Steam ID는 한 번에 100개까지 조회할 수 있습니다"
            );
        }
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new SteamApiException(
                    "스팀 프로필 조회에 필요한 API 키가 없습니다",
                    new IllegalStateException("STEAM_API_KEY 미설정")
            );
        }

        PlayerSummaries body;
        try {
            body = steamWebApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamUser/GetPlayerSummaries/v2/")
                            .queryParam("key", steamApiKey)
                            .queryParam("steamids", String.join(",", uniqueIds))
                            .build())
                    .retrieve()
                    .body(PlayerSummaries.class);
        } catch (RestClientException exception) {
            throw new SteamApiException(
                    "스팀 프로필 조회에 실패했습니다", exception
            );
        }

        if (body == null
                || body.response() == null
                || body.response().players() == null) {
            throw new SteamApiException(
                    "스팀 프로필 응답이 비어 있습니다",
                    new IllegalStateException("Steam player summaries missing")
            );
        }

        Map<String, String> names = new HashMap<>();
        for (Player player : body.response().players()) {
            if (uniqueIds.contains(player.steamid())
                    && player.personaname() != null
                    && !player.personaname().isBlank()) {
                names.put(player.steamid(), player.personaname());
            }
        }
        return names;
    }

    private record PlayerSummaries(PlayerResponse response) {
    }

    private record PlayerResponse(List<Player> players) {
    }

    private record Player(String steamid, String personaname) {
    }
}