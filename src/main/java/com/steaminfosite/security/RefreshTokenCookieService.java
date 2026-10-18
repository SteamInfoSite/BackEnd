package com.steaminfosite.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieService {

    public static final String COOKIE_NAME = "steam_refresh";
    private static final String COOKIE_PATH = "/api/auth/steam";

    private final long refreshTokenDays;
    private final boolean secure;

    public RefreshTokenCookieService(
            @Value("${app.auth.refresh-token-days}") long refreshTokenDays,
            @Value("${app.auth.refresh-cookie-secure:true}") boolean secure
    ) {
        this.refreshTokenDays = refreshTokenDays;
        this.secure = secure;
    }

    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofDays(refreshTokenDays))
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