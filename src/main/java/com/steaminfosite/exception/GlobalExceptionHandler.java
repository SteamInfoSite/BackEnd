package com.steaminfosite.exception;

import com.steaminfosite.dto.response.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SteamUserApiException.class)
    public ResponseEntity<ApiErrorResponse> handleSteamUserApiException(
            SteamUserApiException exception
    ) {
        int statusCode = exception.getStatusCode();

        if (statusCode == 401 || statusCode == 403) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .cacheControl(CacheControl.noStore())
                    .body(new ApiErrorResponse(
                            "STEAM_ACCESS_DENIED",
                            "스팀 정보에 접근할 수 없습니다. 공개 설정과 API 키 상태를 확인해 주세요."
                    ));
        }

        if (statusCode == 429) {
            ResponseEntity.BodyBuilder builder =
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .cacheControl(CacheControl.noStore());

            String retryAfter = normalizeRetryAfter(exception.getRetryAfter());

            if (retryAfter != null) {
                builder.header(HttpHeaders.RETRY_AFTER, retryAfter);
            }

            return builder.body(new ApiErrorResponse(
                    "STEAM_RATE_LIMITED",
                    "스팀 요청 제한에 도달했습니다. 잠시 후 다시 시도해 주세요."
            ));
        }

        return handleSteamApiException(exception);
    }

    @ExceptionHandler(SteamApiException.class)
    public ResponseEntity<ApiErrorResponse> handleSteamApiException(
            SteamApiException exception
    ) {
        //API 키가 포함될 수 있는 원본 예외는 출력하지 않음
        log.warn(
                "스팀 API 요청 처리 실패: {}",
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(new ApiErrorResponse(
                        "STEAM_API_UNAVAILABLE",
                        "스팀 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
                ));
    }

    //Retry-After는 대기 초 또는 HTTP 날짜 형식만 전달
    private String normalizeRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.matches("[0-9]{1,10}")) {
            return trimmed;
        }

        try {
            return ZonedDateTime.parse(
                    trimmed,
                    DateTimeFormatter.RFC_1123_DATE_TIME
            ).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}