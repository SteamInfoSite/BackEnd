package com.steaminfosite.dto.response;

public record ApiErrorResponse(
        String code,
        String message
) {

}