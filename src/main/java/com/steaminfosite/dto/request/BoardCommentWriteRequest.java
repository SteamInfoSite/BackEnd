package com.steaminfosite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardCommentWriteRequest(
        @NotBlank
        @Size(max = 2000)
        String content
) {
}