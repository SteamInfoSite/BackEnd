package com.steaminfosite.client;

import com.steaminfosite.config.SteamOpenIdProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

//Steam OpenID인증에 필요한 로그인 요청 URL생성
@Component
@RequiredArgsConstructor
public class SteamOpenIdClient {
    //요청이 OpenID2.0규격임을 스팀에 알림
    private static final String OPEN_ID_NAMESPACE = "http://specs.openid.net/auth/2.0";
    //사용자 ID를 미리 지정하지않고 스팀이 로그인 사용자를 선택하도록 요청
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";

    private final SteamOpenIdProperties properties;

    //로그인 요청과 callback을 연결할 state를 포함해 스팀 로그인URI 생성
    public URI createLoginUri(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("로그인 state가 필요합니다");
        }
        //스팀이 callback으로 돌려줄 주소에 검증용 state를 함께 포함
        String returnUrl = UriComponentsBuilder
                .fromUriString(properties.getReturnUrl())
                .queryParam("state", state)
                .build()
                .toUriString();

        return UriComponentsBuilder
                .fromUriString(properties.getLoginUrl())
                .queryParam("openid.ns", OPEN_ID_NAMESPACE) //OpenID2.0 규격 선언
                .queryParam("openid.mode", "checkid_setup") //사용자에게 로그인 화면을 표시하도록 요청
                .queryParam("openid.return_to", returnUrl)  //인증 완료 후 돌아올 주소
                .queryParam("openid.realm", properties.getRealm())  //인증을 요청한 서비스의 주소 범위
                .queryParam("openid.identity", IDENTIFIER_SELECT)   //로그인 사용자를 스팀이 선택
                .queryParam("openid.claimed_id", IDENTIFIER_SELECT) //인증된 스팀ID를 반환하도록 요청
                .build()
                .encode()
                .toUri();
    }
}