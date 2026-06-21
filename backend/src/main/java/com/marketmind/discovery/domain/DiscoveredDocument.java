package com.marketmind.discovery.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DiscoveredDocument(
        UUID id,
        DiscoverySourceType sourceType,
        URI sourceUrl,
        URI documentUrl,
        String title,
        String companySymbol,
        DiscoveredDocumentType documentType,
        DiscoveredDocumentStatus status,
        String normalizedUrl,
        Instant firstDiscoveredAt,
        Instant lastSeenAt,
        int seenCount,
        Instant createdAt,
        Instant updatedAt) {
}
