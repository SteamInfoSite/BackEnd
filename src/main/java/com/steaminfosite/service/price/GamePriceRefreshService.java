package com.steaminfosite.service.price;

public interface GamePriceRefreshService {
    // 기본 대상 중 갱신이 필요한 게임의 가격을 수집
    // 반환값은 정상 저장 처리한 개수
    int refreshDefaultPrices();
}