package com.marketmind.documents.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.ExtractionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Extracted PDF text and extraction metadata")
public record DocumentTextExtractionResponse(
        UUID id,
        UUID documentId,
        UUID documentVersionId,
        ExtractionStatus extractionStatus,
        String extractedText,
        Integer pageCount,
        Long characterCount,
        String errorMessage,
        Instant extractedAt,
        Instant createdAt) {
}
