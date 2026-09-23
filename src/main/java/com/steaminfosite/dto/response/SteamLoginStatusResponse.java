package com.steaminfosite.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SteamLoginStatusResponse {
    private boolean loggedIn;
    private String steamId;
}