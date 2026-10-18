package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import com.steaminfosite.dto.response.AccessTokenResponse;
import com.steaminfosite.dto.response.AuthCsrfResponse;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.security.RefreshTokenCookieService;
import com.steaminfosite.security.SteamLoginStateCookieService;
import com.steaminfosite.service.user.RefreshTokenService;
import com.steaminfosite.service.user.SiteUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/steam")
public class SteamAuthController {

    private final SteamOpenIdClient steamOpenIdClient;
    private final SiteUserService siteUserService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final SteamLoginStateCookieService steamLoginStateCookieService;

    @Value("${app.frontend-login-success-url}")
    private String frontendLoginSuccessUrl;

    @GetMapping("/login")
    public ResponseEntity<Void> startLogin() {
        String state = UUID.randomUUID().toString();
        URI steamLoginUri = steamOpenIdClient.createLoginUri(state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(steamLoginUri)
                .header(
                        HttpHeaders.SET_COOKIE,
                        steamLoginStateCookieService.create(state).toString()
                )
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam MultiValueMap<String, String> callbackParams,
            @CookieValue(
                    name = SteamLoginStateCookieService.COOKIE_NAME,
                    required = false
            ) String savedState
    ) {
        String callbackState = callbackParams.getFirst("state");

        if (savedState == null
                || savedState.isBlank()
                || !savedState.equals(callbackState)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 스팀 로그인 요청입니다"
            );
        }

        boolean valid = steamOpenIdClient.verifyCallback(callbackParams);
        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "스팀 로그인 인증에 실패했습니다"
            );
        }

        String steamId;
        try {
            steamId = steamOpenIdClient.extractSteamId(callbackParams);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "스팀 사용자 정보를 확인할 수 없습니다",
                    exception
            );
        }

        SiteUserEntity siteUser = siteUserService.findOrCreate(steamId);

        RefreshTokenService.IssuedTokens tokens =
                refreshTokenService.issueForLogin(siteUser);

        ResponseCookie refreshCookie =
                refreshTokenCookieService.create(tokens.refreshToken());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendLoginSuccessUrl))
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString(),
                        steamLoginStateCookieService.clear().toString()
                )
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(
                    name = RefreshTokenCookieService.COOKIE_NAME,
                    required = false
            ) String rawToken
    ) {
        refreshTokenService.revokeOnLogout(rawToken);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.clear().toString()
                )
                .body("로그아웃되었습니다");
    }

    @GetMapping("/csrf")
    public ResponseEntity<AuthCsrfResponse> csrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthCsrfResponse(
                        csrfToken.getToken(),
                        csrfToken.getHeaderName()
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(
                    name = RefreshTokenCookieService.COOKIE_NAME,
                    required = false
            ) String rawToken
    ) {
        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(rawToken);

        if (result.status() != RefreshTokenService.RotationStatus.SUCCESS) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshTokenCookieService.clear().toString()
                    )
                    .build();
        }

        RefreshTokenService.IssuedTokens tokens = result.tokens();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService
                                .create(tokens.refreshToken()).toString()
                )
                .body(new AccessTokenResponse(
                        tokens.accessToken(),
                        "Bearer",
                        tokens.steamId()
                ));
    }
}