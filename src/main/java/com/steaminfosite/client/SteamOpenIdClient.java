package com.steaminfosite.client;

import com.steaminfosite.config.SteamOpenIdProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Steam OpenID인증에 필요한 로그인 요청 URL생성
@Component
public class SteamOpenIdClient {
    //요청이 OpenID2.0규격임을 스팀에 알림
    private static final String OPEN_ID_NAMESPACE = "http://specs.openid.net/auth/2.0";
    //사용자 ID를 미리 지정하지않고 스팀이 로그인 사용자를 선택하도록 요청
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    //스claimed_id의 경로에서 17자리 Steam64 ID 추출
    private static final Pattern STEAM_ID_PATH_PATTERN = Pattern.compile("^/openid/id/(\\d{17})$");
    private final SteamOpenIdProperties properties;
    private final RestClient steamOpenIdRestClient;

    public SteamOpenIdClient(
            SteamOpenIdProperties properties,
            @Qualifier("steamOpenIdRestClient") //OpenId전용 bean정확히 지정
            RestClient steamOpenIdRestClient
    ) {
        this.properties = properties;
        this.steamOpenIdRestClient = steamOpenIdRestClient;
    }

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
    //Steam callback응답이 실제 Steam에서 전달된것인지 확인
    public boolean verifyCallback(
            MultiValueMap<String, String> callbackParams
    ) {
        if (callbackParams == null || callbackParams.isEmpty()) {
            throw new IllegalArgumentException(
                    "스팀OpenID callback 파라미터가 필요합니다"
            );
        }
        MultiValueMap<String, String> verificationParams = new LinkedMultiValueMap<>();

        //우리 서버가 추가한 state는 제외하고 OpenID파라미터만 복사
        callbackParams.forEach((key, values) -> {
            if (key.startsWith("openid.")) {
                verificationParams.addAll(key, values);
            }
        });
        if (verificationParams.isEmpty()) {
            return false;
        }
        //최초 로그인 응답모드를 스팀 검증요청 모드로 변경
        verificationParams.set(
                "openid.mode",
                "check_authentication"
        );
        String verificationResponse = steamOpenIdRestClient
                .post()
                .uri(URI.create(properties.getLoginUrl()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(verificationParams)
                .retrieve()
                .body(String.class);

        return verificationResponse != null
                && verificationResponse.lines()
                .map(String::trim)
                .anyMatch("is_valid:true"::equals);
    }
    //검증이 완료된 claimed_id에서 Steam64 ID 추출
    public String extractSteamId(
            MultiValueMap<String, String> callbackParams
    ) {
        if (callbackParams == null) {
            throw new IllegalArgumentException(
                    "스팀OpenID callback 파라미터가 필요합니다"
            );
        }
        String claimedId = callbackParams.getFirst("openid.claimed_id");
        if (claimedId == null || claimedId.isBlank()) {
            throw new IllegalArgumentException(
                    "스팀claimed_id가 필요합니다"
            );
        }
        URI claimedIdUri;
        try {
            claimedIdUri = URI.create(claimedId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "스팀claimed_id 형식이 올바르지 않습니다",
                    exception
            );
        }
        String scheme = claimedIdUri.getScheme();
        boolean validScheme = "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme);

        Matcher matcher = STEAM_ID_PATH_PATTERN.matcher(claimedIdUri.getPath());
        if (!validScheme
                || !"steamcommunity.com".equalsIgnoreCase(claimedIdUri.getHost())
                || claimedIdUri.getRawQuery() != null
                || claimedIdUri.getFragment() != null
                || !matcher.matches()) {
            throw new IllegalArgumentException("스팀claimed_id 형식이 올바르지 않습니다");
        }

        return matcher.group(1);
    }
}