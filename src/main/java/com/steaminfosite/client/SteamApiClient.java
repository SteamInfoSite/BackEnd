package com.steaminfosite.client;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.SteamApiWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class SteamApiClient {
    private final RestClient steamRestClient;

    public Optional<SteamApiData> fetchAppDetails(long appId){
        try {
            Map<String, SteamApiWrapper> body = steamRestClient.get()
                    .uri("/appdetails?appids={appId}", appId)
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
}
