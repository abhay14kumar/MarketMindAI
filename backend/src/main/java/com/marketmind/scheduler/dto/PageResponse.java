package com.marketmind.scheduler.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Offset-paginated scheduler response")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }
}
