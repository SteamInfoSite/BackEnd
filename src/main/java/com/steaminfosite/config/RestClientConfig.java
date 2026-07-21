package com.steaminfosite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient steamRestClient(RestClient.Builder builder){
        //타임아웃 설정을 넣기 위한 Http요청 객체 생성
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);    //연결 시도시 3초까지만 기다림
        factory.setReadTimeout(5000);       //응답데이터를 읽을때 5초까지만 기다림, 초과시 실패
        return builder.baseUrl("https://store.steampowered.com/api")
                .requestFactory(factory)
                .build();
    }
}
