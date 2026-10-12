package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceCollectResult;

import java.util.Collection;

public interface GamePriceCollectService {
    //지정한 게임들의 가격을 수집하고 처리 결과 반환
    GamePriceCollectResult collectPrices(Collection<Long> appIds);
}