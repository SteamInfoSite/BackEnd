package com.steaminfosite.dto.response;

import com.steaminfosite.entity.type.BoardCategory;

import java.time.LocalDateTime;

public record BoardPostDetailResponse(
        Long id,
        BoardCategory category,
        String title,
        String contentHtml,
        String authorName,
        boolean isMine,
        long likeCount,
        boolean likedByMe,
        long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
