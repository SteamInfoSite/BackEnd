package com.steaminfosite.dto.response;

//정렬용 출시일을 채우는 데 필요한 기존 데이터
public record GameReleaseDateTarget(
        Long appId,
        String relDate
) {
}