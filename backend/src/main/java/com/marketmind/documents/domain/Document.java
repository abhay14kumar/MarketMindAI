package com.marketmind.documents.domain;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Document(
        UUID id,
        UUID companyId,
        DocumentSource source,
        DocumentType documentType,
        String title,
        URI sourceUrl,
        LocalDate publicationDate,
        String reportingPeriod,
        Integer fiscalYear,
        String quarter,
        DocumentStatus status,
        UUID currentVersionId,
        Instant createdAt,
        Instant updatedAt) {
}
