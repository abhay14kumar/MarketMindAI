package com.marketmind.portfolio.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioImportJob(
        UUID id,
        UUID portfolioId,
        BrokerType brokerType,
        String originalFileName,
        ImportStatus status,
        int totalRows,
        int importedRows,
        int rejectedRows,
        List<RowImportError> rowErrors,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public PortfolioImportJob {
        rowErrors = rowErrors == null ? List.of() : List.copyOf(rowErrors);
    }
}
