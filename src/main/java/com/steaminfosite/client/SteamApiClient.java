package com.steaminfosite.client;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.SteamApiWrapper;
import com.steaminfosite.dto.api.SteamGenreListResponse;
import com.steaminfosite.dto.api.depth1.SteamGenreList;
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

import java.time.Instant;
import java.util.List;
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
                    .uri("/api/appdetails?appids={appId}&l=english", appId)
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

    //기존 전체 목록 조회에서 사용
    public SteamAppList fetchAppList(Long lastAppId, int maxResults) {
        return fetchAppList(lastAppId, maxResults, null);
    }
    //기준 시각이 있으면 해당 시각 이후 변경된 앱만 조회
    public SteamAppList fetchAppList(
            Long lastAppId,
            int maxResults,
            Instant modifiedSince
    ) {
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalStateException("STEAM_API_KEY 미설정");
        }

        if (maxResults < 1 || maxResults > 50_000) {
            throw new IllegalArgumentException(
                    "앱 목록 조회 개수는 1~50000이어야 합니다"
            );
        }

        if (modifiedSince != null
                && (modifiedSince.getEpochSecond() < 0
                || modifiedSince.getEpochSecond() > 4_294_967_295L)) {
            throw new IllegalArgumentException(
                    "앱 목록 변경 기준 시각이 허용 범위를 벗어났습니다"
            );
        }

        try {
            SteamAppListWrapper body = steamWebApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/IStoreService/GetAppList/v1/")
                                .queryParam("key", "{apiKey}")
                                .queryParam("max_results", maxResults);
                        if (lastAppId != null) {
                            uriBuilder.queryParam("last_appid", lastAppId);
                        }
                        if (modifiedSince != null) {
                            uriBuilder.queryParam(
                                    "if_modified_since",
                                    modifiedSince.getEpochSecond()
                            );
                        }
                        return uriBuilder.build(
                                Map.of("apiKey", steamApiKey)
                        );
                    })
                    .retrieve()
                    .body(SteamAppListWrapper.class);

            if (body == null || body.getResponse() == null) {
                throw new SteamApiException(
                        "스팀 앱 목록 응답이 비어 있습니다",
                        null
                );
            }

            return body.getResponse();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
        throw new SteamApiException(
                "스팀 앱 목록 HTTP 오류: status="
                        + e.getStatusCode().value(),
                null
        );

    } catch (ResourceAccessException e) {
        throw new SteamApiException(
                "스팀 앱 목록 연결 오류: 네트워크 또는 타임아웃 확인 필요",
                null
        );

    } catch (RestClientException e) {
        throw new SteamApiException(
                "스팀 앱 목록 응답 처리 오류: type="
                        + e.getClass().getSimpleName(),
                null
        );}
    }

    public List<SteamGenreList> fetchGenreList() {
        try {
            SteamGenreListResponse body = steamRestClient.get()
                    .uri("/api/getgenrelist/")
                    .retrieve()
                    .body(SteamGenreListResponse.class);

            if (body == null
                    || body.getGenres() == null
                    || body.getGenres().isEmpty()) {
                throw new IllegalStateException(
                        "스팀 장르 목록 응답이 비어 있습니다"
                );
            }

            return body.getGenres();

        } catch (RestClientException e) {
            throw new SteamApiException(
                    "스팀 장르 목록 조회에 실패했습니다",
                    e
            );
        }
    }

}
