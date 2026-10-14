package com.steaminfosite.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steaminfosite.dto.api.library.SteamOwnedGamesData;
import com.steaminfosite.dto.api.library.SteamOwnedGamesRequest;
import com.steaminfosite.dto.api.library.SteamOwnedGamesResponse;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamUserApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class SteamLibraryApiClient {
    private final RestClient steamWebApiRestClient;
    private final ObjectMapper objectMapper;

    public SteamLibraryApiClient(
            @Qualifier("steamWebApiRestClient")
            RestClient steamWebApiRestClient,
            ObjectMapper objectMapper
    ) {
        this.steamWebApiRestClient = steamWebApiRestClient;
        this.objectMapper = objectMapper;
    }

    public SteamOwnedGamesData fetchOwnedGames(
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

        SteamOwnedGamesRequest request =
                new SteamOwnedGamesRequest(
                        steamId,
                        true,
                        false
                );

        try {
            String inputJson = objectMapper.writeValueAsString(request);

            SteamOwnedGamesResponse response =
                    steamWebApiRestClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/IPlayerService/GetOwnedGames/v1/")
                                    .queryParam("key", "{apiKey}")
                                    .queryParam("input_json", "{inputJson}")
                                    .build(Map.of(
                                            "apiKey", steamApiKey,
                                            "inputJson", inputJson
                                    ))
                            )
                            .retrieve()
                            .body(SteamOwnedGamesResponse.class);

            if (response == null || response.getResponse() == null) {
                throw new SteamApiException(
                        "스팀 보유 게임 응답에 데이터가 없습니다",
                        null
                );
            }

            return response.getResponse();

        } catch (JsonProcessingException exception) {
            throw new SteamApiException(
                    "스팀 보유 게임 조회 조건을 변환하지 못했습니다",
                    null
            );

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
                    "스팀 보유 게임 조회 중 연결 오류가 발생했습니다",
                    null
            );

        } catch (RestClientException exception) {
            throw new SteamApiException(
                    "스팀 보유 게임 응답을 처리할 수 없습니다",
                    null
            );
        }
    }
}