package com.marketmind.portfolio.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.ImportStatus;

public record PortfolioImportJobResponse(
        UUID id,
        BrokerType brokerType,
        String originalFileName,
        ImportStatus status,
        int totalRows,
        int importedRows,
        int rejectedRows,
        List<RowImportErrorResponse> rowErrors,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
