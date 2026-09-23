package com.steaminfosite.client;

import com.steaminfosite.exception.SteamApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
}