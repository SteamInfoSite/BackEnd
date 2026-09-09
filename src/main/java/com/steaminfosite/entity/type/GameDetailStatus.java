package com.steaminfosite.entity.type;

public enum GameDetailStatus {
    NOT_FETCHED,    //appid 목록만 있고 상세정보는 안가져온 상태
    SUCCESS,        //appdetail 저장 성공
    FAILED,          //appdetail 가져오기 실패
    UPDATE_REQUIRED //appid 목록 재 확인시 last_modified가 바뀌었을때 update허가 하는 상태
}
