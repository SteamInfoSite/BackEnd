package com.steaminfosite.dto.request;

import com.steaminfosite.entity.type.BoardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardPostWriteRequest(
        @NotNull
        BoardCategory category,
        @NotBlank
        @Size(max = 200)
        String title,
        @NotBlank
        String contentHtml
){}
