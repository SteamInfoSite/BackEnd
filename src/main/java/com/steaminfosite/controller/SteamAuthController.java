package com.steaminfosite.controller;

import com.steaminfosite.client.SteamOpenIdClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}