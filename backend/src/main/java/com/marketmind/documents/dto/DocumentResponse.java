package com.marketmind.documents.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Official company document metadata")
public record DocumentResponse(
        UUID id,
        UUID companyId,
        @Schema(example = "NSE") String sourceCode,
        @Schema(example = "National Stock Exchange of India") String sourceName,
        DocumentType documentType,
        @Schema(example = "MarketMind Industries Annual Report 2025-26") String title,
        @Schema(example = "https://example.invalid/marketmind/annual-report-2026.pdf")
        String sourceUrl,
        LocalDate publicationDate,
        @Schema(example = "FY2025-26") String reportingPeriod,
        Integer fiscalYear,
        String quarter,
        DocumentStatus status,
        UUID currentVersionId,
        Instant createdAt,
        Instant updatedAt) {
}
