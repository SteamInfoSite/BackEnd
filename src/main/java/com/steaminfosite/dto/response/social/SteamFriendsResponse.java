package com.steaminfosite.dto.response.social;

import java.time.Instant;
import java.util.List;

public record SteamFriendsResponse(
        List<SteamFriendResponse> friends,
        Instant checkedAt
) {
}