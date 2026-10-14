package com.steaminfosite.dto.response.social;

import com.steaminfosite.dto.api.social.SteamFriend;
import com.steaminfosite.dto.api.social.SteamPlayerSummary;

public record SteamFriendResponse(
        String steamId,
        String nickname,
        String avatarUrl,
        String profileUrl,
        Integer personaState,
        Long lastLogoff,
        String gameId,
        String gameName,
        Long friendSince,
        boolean profileAvailable
) {
    public static SteamFriendResponse from(
            SteamFriend friend,
            SteamPlayerSummary player
    ) {
        //프로필 응답이 누락돼도 친구 목록에는 남겨 둠
        if (player == null) {
            return new SteamFriendResponse(
                    friend.getSteamId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    friend.getFriendSince(),
                    false
            );
        }

        return new SteamFriendResponse(
                friend.getSteamId(),
                player.getPersonaName(),
                player.getAvatarFull(),
                player.getProfileUrl(),
                player.getPersonaState(),
                player.getLastLogoff(),
                player.getGameId(),
                player.getGameExtraInfo(),
                friend.getFriendSince(),
                true
        );
    }
}