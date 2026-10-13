package com.steaminfosite.exception;

public class SteamLibraryUnavailableException extends SteamApiException {

    public SteamLibraryUnavailableException() {
        super(
                "스팀 응답에서 보유 게임 정보를 확인할 수 없습니다",
                null
        );
    }
}