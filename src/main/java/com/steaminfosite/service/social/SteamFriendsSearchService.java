package com.steaminfosite.service.social;

import com.steaminfosite.dto.response.social.SteamFriendsResponse;

public interface SteamFriendsSearchService {
    SteamFriendsResponse getMyFriends(String steamId);
}