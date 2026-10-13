package com.steaminfosite.service.social;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.dto.api.social.SteamFriend;
import com.steaminfosite.dto.api.social.SteamPlayerSummary;
import com.steaminfosite.dto.response.social.SteamFriendResponse;
import com.steaminfosite.dto.response.social.SteamFriendsResponse;
import com.steaminfosite.service.user.SteamApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SteamFriendsSearchServiceImpl
        implements SteamFriendsSearchService {
    private static final int PROFILE_BATCH_SIZE = 100;

    private final SteamApiKeyService steamApiKeyService;
    private final SteamUserApiClient steamUserApiClient;

    @Override
    public SteamFriendsResponse getMyFriends(String steamId) {
        String steamApiKey = steamApiKeyService.getDecryptedApiKey(steamId);

        List<SteamFriend> friends =
                steamUserApiClient.fetchFriendList(
                        steamApiKey,
                        steamId
                );

        //친구가 없으면 프로필 조회 요청을 보내지 않음
        if (friends.isEmpty()) {
            return new SteamFriendsResponse(
                    List.of(),
                    Instant.now()
            );
        }
        //스팀 ID 중복을 제거하면서 친구 목록 순서는 유지
        Map<String, SteamFriend> friendsBySteamId =
                new LinkedHashMap<>();

        for (SteamFriend friend : friends) {
            friendsBySteamId.putIfAbsent(
                    friend.getSteamId(),
                    friend
            );
        }

        List<String> friendSteamIds = new ArrayList<>(friendsBySteamId.keySet());

        Map<String, SteamPlayerSummary> profilesBySteamId = new HashMap<>();
        //프로필은 최대 100명씩 묶어서 조회
        for (int start = 0;
             start < friendSteamIds.size();
             start += PROFILE_BATCH_SIZE) {

            int end = Math.min(
                    start + PROFILE_BATCH_SIZE,
                    friendSteamIds.size()
            );

            List<String> batchSteamIds = friendSteamIds.subList(start, end);

            List<SteamPlayerSummary> players =
                    steamUserApiClient.fetchPlayerSummaries(
                            steamApiKey,
                            batchSteamIds
                    );

            for (SteamPlayerSummary player : players) {
                if (player == null
                        || player.getSteamId() == null
                        || !batchSteamIds.contains(player.getSteamId())) {
                    continue;
                }

                profilesBySteamId.put(
                        player.getSteamId(),
                        player
                );
            }
        }
        //친구 목록과 프로필을 스팀 ID로 연결
        List<SteamFriendResponse> friendResponses =
                friendsBySteamId.values().stream()
                        .map(friend -> SteamFriendResponse.from(
                                friend,
                                profilesBySteamId.get(friend.getSteamId())
                        ))
                        .toList();

        return new SteamFriendsResponse(
                friendResponses,
                Instant.now()
        );
    }
}