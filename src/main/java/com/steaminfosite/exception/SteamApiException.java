package com.steaminfosite.exception;

//Steam API 호출 중 발생한 HTTP 및 네트워크 오류를 서비스 계층으로 전달
public class SteamApiException extends RuntimeException {

    //원본 예외를 cause로 보존해 실제 발생 원인과 stack trace를 확인할 수 있게 함
    public SteamApiException(String message, Throwable cause) {
        super(message, cause);
    }
}