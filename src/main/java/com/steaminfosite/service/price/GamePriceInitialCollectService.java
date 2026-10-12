package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceCollectResult;

public interface GamePriceInitialCollectService {
    //아직 가격을 정상 확인하지 못한 출시된 유료 게임을 순차 수집
    GamePriceCollectResult collectInitialPrices();
}