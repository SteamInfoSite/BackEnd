package com.steaminfosite.dto.response.social;

import com.steaminfosite.dto.api.social.SteamPlayerSummary;

import java.time.Instant;

public record SteamProfileResponse(
        String steamId,
        String nickname,
        String avatarUrl,
        String profileUrl,
        Integer personaState,
        Integer communityVisibilityState,
        Long lastLogoff,
        String gameId,
        String gameName,
        Instant checkedAt
) {

    public static SteamProfileResponse from(
            SteamPlayerSummary player,
            Instant checkedAt
    ) {
        return new SteamProfileResponse(
                player.getSteamId(),
                player.getPersonaName(),
                player.getAvatarFull(),
                player.getProfileUrl(),
                player.getPersonaState(),
                player.getCommunityVisibilityState(),
                player.getLastLogoff(),
                player.getGameId(),
                player.getGameExtraInfo(),
                checkedAt
        );
    }
}