package com.marketmind.discovery.dto;

import java.util.List;

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
