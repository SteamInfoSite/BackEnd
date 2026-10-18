package com.steaminfosite.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SteamLoginStateCookieService {
    public static final String COOKIE_NAME = "steam_login_state";
    private static final String COOKIE_PATH = "/api/auth/steam";

    private final boolean secure;

    public SteamLoginStateCookieService(
            @Value("${app.auth.refresh-cookie-secure:true}") boolean secure
    ) {
        this.secure = secure;
    }

    public ResponseCookie create(String state) {
        return ResponseCookie.from(COOKIE_NAME, state)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMinutes(15))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}