package com.steaminfosite.client;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.SteamApiWrapper;
import com.steaminfosite.dto.applist.SteamAppList;
import com.steaminfosite.dto.applist.SteamAppListWrapper;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamRateLimitException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class SteamApiClient {
    private final RestClient steamRestClient;
    private final RestClient steamWebApiRestClient;
    private final String steamApiKey;

    public Optional<SteamApiData> fetchAppDetails(long appId){
        try {
            Map<String, SteamApiWrapper> body = steamRestClient.get()
                    .uri("/api/appdetails?appids={appId}", appId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            SteamApiWrapper wrapper = body.get(String.valueOf(appId));

            if (wrapper == null) {
                System.out.println("Steam 상세 API wrapper 없음 appId = " + appId);
                return Optional.empty();
            }
            if (!wrapper.isSuccess()) {
                System.out.println("Steam 상세 API success=false appId = " + appId);
                return Optional.empty();
            }
            return Optional.of(wrapper.getData());

        } catch (HttpClientErrorException | HttpServerErrorException e) {   //400대 에러 | 500대 에러 발생시
            System.out.println("Steam 상세 API HTTP 실패 appId = " + appId);
            System.out.println("status = " + e.getStatusCode());
            if (e.getStatusCode().value() == 429) { //429 별도 처리
                throw new SteamRateLimitException(
                        appId,
                        10_000L,
                        e
                );
            }
            throw new SteamApiException(    //429이외
                    "Steam 상세 API HTTP 오류 appId=" + appId,
                    e
            );
        } catch (ResourceAccessException e) {   //I/O하는 도중 에러 발생시
            System.out.println("Steam 상세 API 연결/타임아웃 실패 appId = " + appId);
            System.out.println("message = " + e.getMessage());
            throw new SteamApiException(
                    "Steam 상세 API 연결 오류 appId=" + appId,
                    e
            );
        } catch (RestClientException e) {
            //예상못한 응답형식도 전체수집을 중단하지않고 API오류로 기록
            System.out.println("Steam 상세 API 응답 변환 실패 appId = " + appId);
            System.out.println("message = " + e.getMessage());

            throw new SteamApiException(
                "Steam 상세 API 응답 처리 오류 appId=" + appId,
                e
            );
        }
    }

    public SteamApiClient(
            @Qualifier("steamRestClient") RestClient steamRestClient,
            @Qualifier("steamWebApiRestClient") RestClient steamWebApiRestClient,
            @Value("${steam.api.key:}") String steamApiKey
    ) {
        this.steamRestClient = steamRestClient;
        this.steamWebApiRestClient = steamWebApiRestClient;
        this.steamApiKey = steamApiKey;
    }

    public SteamAppList fetchAppList(Long lastAppId, int maxResults) {
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalStateException("STEAM_API_KEY미설정");
        }

        SteamAppListWrapper body = steamWebApiRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/IStoreService/GetAppList/v1/")
                            .queryParam("key", steamApiKey)
                            .queryParam("max_results", maxResults);

                    if (lastAppId != null) {
                        uriBuilder.queryParam("last_appid", lastAppId);
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .body(SteamAppListWrapper.class);

        if (body == null || body.getResponse() == null) {
            throw new IllegalStateException("Steam 앱 목록 응답이 빔");
        }

        return body.getResponse();
    }

}
