package com.steaminfosite.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SteamOpenIdPropertiesTest {

    @Autowired
    private SteamOpenIdProperties properties;

    @Test
    void configOpenId() { //로그인 구현 설정 기반 확인
        assertThat(properties.getLoginUrl())
                .isEqualTo("https://steamcommunity.com/openid/login");

        assertThat(properties.getRealm())
                .isEqualTo("http://localhost:8080/");

        assertThat(properties.getReturnUrl())
                .isEqualTo("http://localhost:8080/api/auth/steam/callback");
    }
}