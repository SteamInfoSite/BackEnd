package com.steaminfosite.service;

import com.steaminfosite.dto.response.GameDetailCollectResult;

import java.util.List;

public interface GameDetailCollectService {
    //지정한 개수만큼 상세정보를 수집하고 상태별 결과를 반환
    GameDetailCollectResult collectNotFetched(int limit);
    //수집 대상이 없어질 때까지 반복하고 전체 결과를 반환
    GameDetailCollectResult collectAllNotFetched(int limit);
    //지정한 게임들의 상세정보를 다시 수집
    GameDetailCollectResult collectByAppIds(List<Long> appIds);
}
