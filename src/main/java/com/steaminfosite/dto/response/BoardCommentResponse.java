package com.steaminfosite.dto.response;

import java.time.LocalDateTime;

public record BoardCommentResponse(
        Long id,
        Long postId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
