package com.marketmind.discovery.dto;

import java.time.Instant;
import java.util.UUID;

public record DiscoveredDocumentResponse(
        UUID id,
        String sourceType,
        String sourceUrl,
        String documentUrl,
        String title,
        String companySymbol,
        String documentType,
        String status,
        String normalizedUrl,
        Instant firstDiscoveredAt,
        Instant lastSeenAt,
        int seenCount,
        Instant createdAt,
        Instant updatedAt) {
}
