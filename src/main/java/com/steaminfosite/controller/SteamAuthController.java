package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/steam")
public class SteamAuthController {

    //로그인 요청과 callback을 연결하기위해 세션에 저장할 키
    static final String STEAM_LOGIN_STATE_SESSION_KEY = "steamLoginState";

    private final SteamOpenIdClient steamOpenIdClient;

    @GetMapping("/login")
    public ResponseEntity<Void> startLogin(HttpSession session) {
        //로그인 요청마다 예측하기 어려운 새로운 state생성
        String state = UUID.randomUUID().toString();
        //callback에서 같은 브라우저의 로그인 요청인지 비교하기 위해 저장
        session.setAttribute(
                STEAM_LOGIN_STATE_SESSION_KEY,
                state
        );
        URI steamLoginUri = steamOpenIdClient.createLoginUri(state);
        //브라우저를 생성된 Steam 로그인 주소로 이동
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(steamLoginUri)
                .build();
    }
    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam MultiValueMap<String, String> callbackParams,
            HttpSession session
    ) {
        //callback으로 돌아온 state와 로그인 시작시 저장한 state조회
        String callbackState = callbackParams.getFirst("state");
        String savedState = (String) session.getAttribute(
                STEAM_LOGIN_STATE_SESSION_KEY
        );
        //세션이 없거나 state가 다르면 우리 서버에서 시작한 로그인 요청이 아님
        if (savedState == null || !savedState.equals(callbackState)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 스팀 로그인 요청입니다"
            );
        }
        //검증에 사용한 state를 제거해 동일 callback의 재사용 방지
        session.removeAttribute(STEAM_LOGIN_STATE_SESSION_KEY);
        //OpenID응답을 스팀에 다시 보내 서명 유효성 확인
        boolean valid = steamOpenIdClient.verifyCallback(callbackParams);

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "스팀 로그인 인증에 실패했습니다"
            );
        }

        return ResponseEntity.ok("스팀OpenID 인증에 성공했습니다");
    }
}