package com.marketmind.documents.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentTextExtraction(
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
