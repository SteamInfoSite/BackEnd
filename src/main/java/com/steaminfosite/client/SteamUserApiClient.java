package com.steaminfosite.client;

import com.steaminfosite.dto.api.social.SteamFriend;
import com.steaminfosite.dto.api.social.SteamFriendsListResponse;
import com.steaminfosite.dto.api.social.SteamPlayerSummariesResponse;
import com.steaminfosite.dto.api.social.SteamPlayerSummary;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamUserApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.util.List;

@Component
public class SteamUserApiClient {
    private final RestClient steamWebApiRestClient;

    public SteamUserApiClient(
            @Qualifier("steamWebApiRestClient")
            RestClient steamWebApiRestClient
    ) {
        this.steamWebApiRestClient = steamWebApiRestClient;
    }

    public boolean validateApiKey(
            String steamApiKey,
            String steamId
    ) {
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "스팀API 키가 필요합니다"
            );
        }
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Steam64 ID가 필요합니다"
            );
        }
        try {
            steamWebApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamUser/GetPlayerSummaries/v2/")
                            .queryParam("key", steamApiKey)
                            .queryParam("steamids", steamId)
                            .build()
                    )
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException exception) {
            int statusCode = exception.getStatusCode().value();
            //인증 실패는 스팀API 키가 유효하지 않은 것으로 처리
            if (statusCode == 401 || statusCode == 403) {
                return false;
            }
            throw new SteamApiException(
                    "스팀API키 검증 중 HTTP오류가 발생했습니다",
                    exception
            );
        } catch (HttpServerErrorException exception) {
            throw new SteamApiException(
                    "스팀API키 검증 중 스팀서버 오류가 발생했습니다",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new SteamApiException(
                    "스팀API키 검증 중 연결 오류가 발생했습니다",
                    exception
            );
        } catch (RestClientException exception) {
            throw new SteamApiException(
                    "스팀API키 검증 응답을 처리할 수 없습니다",
                    exception
            );
        }
    }

    //본인 또는 친구들의 프로필을 한 번에 조회
    public List<SteamPlayerSummary> fetchPlayerSummaries(
            String steamApiKey,
            List<String> steamIds
    ) {
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "스팀 API 키가 필요합니다"
            );
        }

        if (steamIds == null) {
            throw new IllegalArgumentException(
                    "조회할 스팀 ID 목록이 필요합니다"
            );
        }

        if (steamIds.isEmpty()) {
            return List.of();
        }

        if (steamIds.stream().anyMatch(
                steamId -> steamId == null
                        || !steamId.matches("[0-9]{17}")
        )) {
            throw new IllegalArgumentException(
                    "스팀 ID는 17자리 숫자 문자열이어야 합니다"
            );
        }

        List<String> uniqueSteamIds = steamIds.stream()
                .distinct()
                .toList();

        if (uniqueSteamIds.size() > 100) {
            throw new IllegalArgumentException(
                    "프로필은 한 번에 최대 100명까지 조회할 수 있습니다"
            );
        }

        try {
            SteamPlayerSummariesResponse response =
                    steamWebApiRestClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/ISteamUser/GetPlayerSummaries/v2/")
                                    .queryParam("key", steamApiKey)
                                    .queryParam(
                                            "steamids",
                                            String.join(",", uniqueSteamIds)
                                    )
                                    .build()
                            )
                            .retrieve()
                            .body(SteamPlayerSummariesResponse.class);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getPlayers() == null) {
                throw new SteamApiException(
                        "스팀 프로필 응답에 사용자 목록이 없습니다",
                        null
                );
            }

            return response.getResponse().getPlayers();

        } catch (RestClientResponseException exception) {
            String retryAfter = exception.getResponseHeaders() == null
                    ? null
                    : exception.getResponseHeaders().getFirst("Retry-After");

            throw new SteamUserApiException(
                    exception.getStatusCode().value(),
                    retryAfter
            );

        } catch (ResourceAccessException exception) {
            throw new SteamApiException(
                    "스팀 프로필 조회 중 연결 오류가 발생했습니다",
                    null
            );

        } catch (RestClientException exception) {
            throw new SteamApiException(
                    "스팀 프로필 응답을 처리할 수 없습니다",
                    null
            );
        }
    }

    //해당 사용자의 친구 목록 조회
    public List<SteamFriend> fetchFriendList(
            String steamApiKey,
            String steamId
    ) {
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "스팀 API 키가 필요합니다"
            );
        }

        if (steamId == null || !steamId.matches("[0-9]{17}")) {
            throw new IllegalArgumentException(
                    "스팀 ID는 17자리 숫자 문자열이어야 합니다"
            );
        }

        try {
            SteamFriendsListResponse response =
                    steamWebApiRestClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/ISteamUser/GetFriendList/v1/")
                                    .queryParam("key", steamApiKey)
                                    .queryParam("steamid", steamId)
                                    .queryParam("relationship", "friend")
                                    .build()
                            )
                            .retrieve()
                            .body(SteamFriendsListResponse.class);

            if (response == null
                    || response.getFriendsList() == null
                    || response.getFriendsList().getFriends() == null) {
                throw new SteamApiException(
                        "스팀 친구 목록 응답에 목록 정보가 없습니다",
                        null
                );
            }

            List<SteamFriend> friends = response.getFriendsList().getFriends();

            if (friends.stream().anyMatch(
                    friend -> friend == null
                            || friend.getSteamId() == null
                            || !friend.getSteamId().matches("[0-9]{17}")
            )) {
                throw new SteamApiException(
                        "스팀 친구 목록에 올바르지 않은 사용자 정보가 있습니다",
                        null
                );
            }

            return friends;

        } catch (RestClientResponseException exception) {
            String retryAfter = exception.getResponseHeaders() == null
                    ? null
                    : exception.getResponseHeaders().getFirst("Retry-After");

            throw new SteamUserApiException(
                    exception.getStatusCode().value(),
                    retryAfter
            );

        } catch (ResourceAccessException exception) {
            throw new SteamApiException(
                    "스팀 친구 목록 조회 중 연결 오류가 발생했습니다",
                    null
            );

        } catch (RestClientException exception) {
            throw new SteamApiException(
                    "스팀 친구 목록 응답을 처리할 수 없습니다",
                    null
            );
        }
    }
}