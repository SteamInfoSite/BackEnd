package com.steaminfosite.dto.response;

import com.steaminfosite.entity.type.BoardCategory;

import java.time.LocalDateTime;

public record BoardPostDetailResponse(
        Long id,
        BoardCategory category,
        String title,
        String contentHtml,
        String authorName,
        long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
