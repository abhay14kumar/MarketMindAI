package com.marketmind.portfolio.dto;

public record RowImportErrorResponse(
        int rowNumber,
        String message) {
}
