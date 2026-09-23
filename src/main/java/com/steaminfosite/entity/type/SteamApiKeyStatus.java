package com.steaminfosite.entity.type;

public enum SteamApiKeyStatus {
    //아직 API 키를 등록하지 않음
    NOT_REGISTERED,
    //스팀API에서 유효성이 확인된 키
    VALID,
    //입력했지만 유효하지 않은 키
    INVALID
}