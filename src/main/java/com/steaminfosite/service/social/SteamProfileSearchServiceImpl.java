package com.steaminfosite.service.social;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.dto.api.social.SteamPlayerSummary;
import com.steaminfosite.dto.response.social.SteamProfileResponse;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.service.user.SteamApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SteamProfileSearchServiceImpl
        implements SteamProfileSearchService {
    private final SteamApiKeyService steamApiKeyService;
    private final SteamUserApiClient steamUserApiClient;

    @Override
    public SteamProfileResponse getMyProfile(String steamId) {
        String steamApiKey = steamApiKeyService.getDecryptedApiKey(steamId);

        List<SteamPlayerSummary> players =
                steamUserApiClient.fetchPlayerSummaries(
                        steamApiKey,
                        List.of(steamId)
                );

        //응답 중 요청한 본인의 스팀 ID와 일치하는 프로필 선택
        SteamPlayerSummary myProfile = players.stream()
                .filter(Objects::nonNull)
                .filter(player ->
                        steamId.equals(player.getSteamId())
                )
                .findFirst()
                .orElseThrow(() -> new SteamApiException(
                        "스팀 응답에서 본인 프로필을 찾을 수 없습니다",
                        null
                ));

        return SteamProfileResponse.from(
                myProfile,
                Instant.now()
        );
    }
}