package com.steaminfosite.client;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.SteamApiWrapper;
import com.steaminfosite.dto.applist.SteamAppList;
import com.steaminfosite.dto.applist.SteamAppListWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

            if (wrapper == null || !wrapper.isSuccess()) {
                return Optional.empty();    //비어있거나 Failed라서 빈값 반환
            }
            return Optional.of(wrapper.getData());

        } catch (HttpClientErrorException | HttpServerErrorException e) {   //400대 에러 | 500대 에러 발생시
            return Optional.empty();

        } catch (ResourceAccessException e) {   //I/O하는 도중 에러 발생시
            return Optional.empty();
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
