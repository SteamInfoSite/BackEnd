package com.steaminfosite.exception;

//API가 429반환시 재시도 대기 시간과 함께 전달하는 전용 예외
public class SteamRateLimitException extends SteamApiException {

    private final long retryAfterMillis;

    public SteamRateLimitException(
            long appId,
            long retryAfterMillis,
            Throwable cause
    ) {
        super("Steam API 요청 제한 appId=" + appId, cause);
        this.retryAfterMillis = retryAfterMillis;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}