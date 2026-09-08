package com.steaminfosite.entity.type;

public enum GameDetailStatus {
    NOT_FETCHED,    //appid 목록만 있고 상세정보는 안가져온 상태
    SUCCESS,        //appdetail 저장 성공
    FAILED          //appdetail 가져오기 실패
}
