package com.steaminfosite.dto.response;

import com.steaminfosite.entity.type.BoardCategory;

import java.time.LocalDateTime;

public record BoardPostListResponse(
        Long id,
        BoardCategory category,
        String title,
        String authorName,
        long viewCount,
        long commentCount,
        long likeCount,
        LocalDateTime createdAt
) {
}
