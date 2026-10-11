package com.steaminfosite.service.price;

import java.util.Collection;

public interface GamePriceCollectService {
    //지정한 게임들의 가격을 수집하고 정상 저장 처리 개수 반환
    int collectPrices(Collection<Long> appIds);
}