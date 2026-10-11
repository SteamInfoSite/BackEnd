package com.steaminfosite.dto.response;

//가격 수집의 정상 저장 개수와 요청 제한 발생 여부
public record GamePriceCollectResult(
        int savedCount,
        boolean rateLimited
) {
}