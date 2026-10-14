package com.steaminfosite.dto.response;

import org.springframework.data.domain.Page;
import java.util.List;

public record BoardPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> BoardPageResponse<T> from(Page<T> result) {
        return new BoardPageResponse<>(
                result.getContent(),
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}