package com.steaminfosite.service.social;

import com.steaminfosite.dto.response.social.SteamProfileResponse;

public interface SteamProfileSearchService {
    SteamProfileResponse getMyProfile(String steamId);
}