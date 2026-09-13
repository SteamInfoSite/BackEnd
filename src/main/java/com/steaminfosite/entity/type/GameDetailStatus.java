package com.steaminfosite.entity.type;

public enum GameDetailStatus {
    NOT_FETCHED,    //appid 목록만 있고 상세정보는 안가져온 상태
    SUCCESS,        //appdetail 저장 성공
    NOT_FOUND,       // appdetails success=false
    RATE_LIMITED,   //appdetail 429 TOO_MANY_REQUESTS로 인한 가져오기 실패(재시도 필요)
    API_ERROR,         // 일시적 API/네트워크 오류
    UPDATE_REQUIRED //appid 목록 재 확인시 last_modified가 바뀌었을때 update허가 하는 상태
}
