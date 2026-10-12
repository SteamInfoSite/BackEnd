package com.steaminfosite.client;

import com.steaminfosite.exception.SteamApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class SteamProfileClient {
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
                            .queryParam("steamids", steamId)
                            .build())
                    .retrieve()
                    .body(PlayerSummaries.class);
        } catch (RestClientException exception) {
            throw new SteamApiException("스팀 프로필 조회에 실패했습니다", exception);
        }

        if (body != null
                && body.response() != null
                && body.response().players() != null) {
            for (Player player : body.response().players()) {
                if (steamId.equals(player.steamid())
                        && player.personaname() != null
                        && !player.personaname().isBlank()) {
                    return player.personaname();
                }
            }
        }

        throw new SteamApiException(
                "스팀 프로필 응답에 표시 이름이 없습니다",
                new IllegalStateException("Steam player summary missing")
        );
    }

    private record PlayerSummaries(PlayerResponse response) {
    }

    private record PlayerResponse(List<Player> players) {
    }

    private record Player(String steamid, String personaname) {
    }
}