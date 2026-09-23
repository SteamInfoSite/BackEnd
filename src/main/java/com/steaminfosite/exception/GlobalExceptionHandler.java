package com.steaminfosite.exception;

import com.steaminfosite.dto.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SteamApiException.class)
    public ResponseEntity<ApiErrorResponse> handleSteamApiException(
            SteamApiException exception
    ) {
        //API키가 포함될 수 있는 원본 요청 주소는 기록하지 않음
        log.warn(
                "스팀API 요청 처리 실패: {}",
                exception.getMessage()
        );
        ApiErrorResponse response = new ApiErrorResponse(
                "STEAM_API_UNAVAILABLE",
                "스팀 서비스에 일시적으로 연결할 수 없습니다. 잠시후 다시 시도해 주세요"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}