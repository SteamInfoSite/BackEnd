package com.steaminfosite.controller;

import com.jayway.jsonpath.JsonPath;
import com.steaminfosite.client.SteamOpenIdClient;
import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.RefreshTokenCookieService;
import com.steaminfosite.security.SteamLoginStateCookieService;
import com.steaminfosite.service.user.RefreshTokenService;
import com.steaminfosite.service.user.SiteUserService;
import java.net.URI;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties =
        {"app.frontend-login-success-url=http://localhost:5173/auth/steam/complete"}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SteamAuthIntegrationTest {    //H2에 사용자까지 저장되는지 확인하는 통합 테스트

    private static final String STEAM_ID = "76561198000000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteUserRepository siteUserRepository;

    @MockitoBean
    private SteamOpenIdClient steamOpenIdClient;

    @Autowired
    private SiteUserService siteUserService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("로그인 시작 시 state 쿠키를 발급하고 콜백에서 검증")
    void login() throws Exception {
        given(steamOpenIdClient.createLoginUri(any()))
                .willReturn(URI.create("https://steamcommunity.com/openid/login"));

        MvcResult loginStart = mockMvc.perform(get("/api/auth/steam/login"))
                .andExpect(status().isFound())
                .andReturn();

        String stateCookieHeader = loginStart.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(
                        SteamLoginStateCookieService.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        String prefix = SteamLoginStateCookieService.COOKIE_NAME + "=";
        String state = stateCookieHeader.substring(
                prefix.length(),
                stateCookieHeader.indexOf(';')
        );

        assertThat(state).isNotBlank();
        assertThat(stateCookieHeader)
                .contains("HttpOnly", "SameSite=Lax", "Path=/api/auth/steam");
        assertThat(loginStart.getRequest().getSession(false)).isNull();

        given(steamOpenIdClient.verifyCallback(any())).willReturn(true);
        given(steamOpenIdClient.extractSteamId(any())).willReturn(STEAM_ID);

        MvcResult callbackResult = mockMvc.perform(
                        get("/api/auth/steam/callback")
                                .cookie(new Cookie(
                                        SteamLoginStateCookieService.COOKIE_NAME,
                                        state
                                ))
                                .param("state", state)
                                .param("openid.mode", "id_res")
                                .param(
                                        "openid.claimed_id",
                                        "https://steamcommunity.com/openid/id/"
                                                + STEAM_ID
                                ))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "http://localhost:5173/auth/steam/complete"
                ))
                .andReturn();

        String refreshCookieHeader = callbackResult.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(
                        RefreshTokenCookieService.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        assertThat(refreshCookieHeader).contains(
                "HttpOnly",
                "Secure",
                "SameSite=Lax",
                "Path=/api/auth/steam",
                "Max-Age=604800"
        );

        String clearedStateCookie = callbackResult.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(
                        SteamLoginStateCookieService.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        assertThat(clearedStateCookie).contains("Max-Age=0");

        assertThat(callbackResult.getRequest().getSession(false)).isNull();

        Optional<SiteUserEntity> savedUser = siteUserRepository.findBySteamId(STEAM_ID);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getSteamId()).isEqualTo(STEAM_ID);
        assertThat(savedUser.get().getRole()).isEqualTo(SiteUserRole.USER);
    }

    @Test
    @DisplayName("CSRF 조회 시 토큰과 검증 쿠키를 반환")
    void csrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/steam/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(result -> {
                    var cookie = result.getResponse().getCookie("XSRF-TOKEN");

                    assertThat(cookie).isNotNull();
                    assertThat(cookie.isHttpOnly()).isTrue();
                    assertThat(cookie.getSecure()).isTrue();
                    assertThat(cookie.getPath()).isEqualTo("/api/auth/steam");
                    assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
                });
    }

    @Test
    @DisplayName("CSRF 토큰 없이 재발급 요청 시 차단")
    void refreshWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/steam/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF와 재발급 쿠키가 유효하면 접근 토큰과 새 쿠키를 반환")
    void refreshSuccess() throws Exception {
        var user = siteUserService.findOrCreate(STEAM_ID);
        var issued = refreshTokenService.issueForLogin(user);

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/steam/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = JsonPath.read(
                csrfResult.getResponse().getContentAsString(), "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        MvcResult refreshResult = mockMvc.perform(
                        post("/api/auth/steam/refresh")
                                .cookie(
                                        csrfCookie,
                                        new Cookie(
                                                RefreshTokenCookieService.COOKIE_NAME,
                                                issued.refreshToken()))
                                .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(
                refreshResult.getResponse().getContentAsString(),
                "$.accessToken");
        assertThat(jwtDecoder.decode(accessToken).getSubject())
                .isEqualTo(STEAM_ID);

        String newCookie = refreshResult.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(
                        RefreshTokenCookieService.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        assertThat(newCookie).contains("HttpOnly", "Secure", "SameSite=Lax");
        assertThat(newCookie).doesNotContain(issued.refreshToken());
    }

    @Test
    @DisplayName("재발급 쿠키가 없으면 401을 반환하고 쿠키를 삭제")
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/steam/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = JsonPath.read(
                csrfResult.getResponse().getContentAsString(), "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(post("/api/auth/steam/refresh")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(result -> {
                    String clearedCookie = result.getResponse()
                            .getHeaders(HttpHeaders.SET_COOKIE)
                            .stream()
                            .filter(value -> value.startsWith(
                                    RefreshTokenCookieService.COOKIE_NAME + "="))
                            .findFirst()
                            .orElseThrow();

                    assertThat(clearedCookie).contains(
                            "Max-Age=0",
                            "Path=/api/auth/steam"
                    );
                });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("이전 재발급 토큰을 재사용하면 새 토큰도 무효화")
    void reusedRefreshTokenRevokesRotatedToken() throws Exception {
        String steamId = "76561198000000001";
        var user = siteUserService.findOrCreate(steamId);
        var issued = refreshTokenService.issueForLogin(user);

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/steam/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = JsonPath.read(
                csrfResult.getResponse().getContentAsString(), "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        MvcResult firstRefresh = mockMvc.perform(
                        post("/api/auth/steam/refresh")
                                .cookie(csrfCookie, new Cookie(
                                        RefreshTokenCookieService.COOKIE_NAME,
                                        issued.refreshToken()))
                                .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        String cookieHeader = firstRefresh.getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(value -> value.startsWith(
                        RefreshTokenCookieService.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        String prefix = RefreshTokenCookieService.COOKIE_NAME + "=";
        String rotatedToken = cookieHeader.substring(
                prefix.length(), cookieHeader.indexOf(';'));

        mockMvc.perform(post("/api/auth/steam/refresh")
                        .cookie(csrfCookie, new Cookie(
                                RefreshTokenCookieService.COOKIE_NAME,
                                issued.refreshToken()))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/steam/refresh")
                        .cookie(csrfCookie, new Cookie(
                                RefreshTokenCookieService.COOKIE_NAME,
                                rotatedToken))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 로그아웃하면 차단")
    void logoutWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/steam/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("로그아웃하면 재발급 토큰을 폐기하고 쿠키를 삭제")
    void logoutRevokesRefreshToken() throws Exception {
        String steamId = "76561198000000002";
        var user = siteUserService.findOrCreate(steamId);
        var issued = refreshTokenService.issueForLogin(user);

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/steam/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = JsonPath.read(
                csrfResult.getResponse().getContentAsString(), "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(post("/api/auth/steam/logout")
                        .cookie(
                                csrfCookie,
                                new Cookie(
                                        RefreshTokenCookieService.COOKIE_NAME,
                                        issued.refreshToken()))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(result -> {
                    String clearedCookie = result.getResponse()
                            .getHeaders(HttpHeaders.SET_COOKIE)
                            .stream()
                            .filter(value -> value.startsWith(
                                    RefreshTokenCookieService.COOKIE_NAME + "="))
                            .findFirst()
                            .orElseThrow();

                    assertThat(clearedCookie).contains(
                            "Max-Age=0",
                            "Path=/api/auth/steam");
                });

        mockMvc.perform(post("/api/auth/steam/refresh")
                        .cookie(
                                csrfCookie,
                                new Cookie(
                                        RefreshTokenCookieService.COOKIE_NAME,
                                        issued.refreshToken()))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized());
    }
}