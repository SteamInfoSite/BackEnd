package com.steaminfosite.client;

import com.steaminfosite.dto.api.price.SteamPriceWrapper;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.exception.SteamRateLimitException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SteamPriceApiClient {

    //프로젝트에서 우선 사용할 요청 묶음 크기
    public static final int MAX_BATCH_SIZE = 5;

    private final RestClient steamRestClient;

    public SteamPriceApiClient(
            @Qualifier("steamRestClient")
            RestClient steamRestClient
    ) {
        this.steamRestClient = steamRestClient;
    }

    public Map<String, SteamPriceWrapper> fetchPrices(
            Collection<Long> appIds
    ) {
        if (appIds == null) {
            throw new IllegalArgumentException(
                    "게임 ID 목록이 필요합니다"
            );
        }

        if (appIds.isEmpty()) {
            return Map.of();
        }

        if (appIds.stream().anyMatch(
                appId -> appId == null || appId <= 0
        )) {
            throw new IllegalArgumentException(
                    "올바른 스팀 게임 ID가 필요합니다"
            );
        }

        List<Long> uniqueAppIds = appIds.stream()
                .distinct()
                .toList();

        if (uniqueAppIds.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "가격은 한 번에 최대 "
                            + MAX_BATCH_SIZE
                            + "개씩 요청해 주세요"
            );
        }

        String joinedAppIds = uniqueAppIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        try {
            Map<String, SteamPriceWrapper> body =
                    steamRestClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/appdetails")
                                    .queryParam("appids", joinedAppIds)
                                    .queryParam("filters", "price_overview")
                                    .queryParam("cc", "kr")
                                    .queryParam("l", "english")
                                    .build()
                            )
                            .retrieve()
                            .body(new ParameterizedTypeReference<
                                    Map<String, SteamPriceWrapper>
                                    >() {});

            if (body == null || body.isEmpty()) {
                throw new SteamApiException(
                        "스팀 가격 응답이 비어 있습니다. appIds="
                                + joinedAppIds,
                        null
                );
            }

            return body;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            if (e.getStatusCode().value() == 429) {
                throw new SteamRateLimitException(
                        "스팀 가격 API 요청 제한 appIds="
                                + joinedAppIds,
                        10_000L,
                        e
                );
            }

            throw new SteamApiException(
                    "스팀 가격 API HTTP 오류 appIds="
                            + joinedAppIds,
                    e
            );

        } catch (ResourceAccessException e) {
            throw new SteamApiException(
                    "스팀 가격 API 연결 오류 appIds="
                            + joinedAppIds,
                    e
            );

        } catch (RestClientException e) {
            throw new SteamApiException(
                    "스팀 가격 API 응답 처리 오류 appIds="
                            + joinedAppIds,
                    e
            );
        }
    }
}