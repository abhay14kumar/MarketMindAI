package com.marketmind.discovery.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DiscoveryJob(
        UUID id,
        DiscoverySourceType sourceType,
        URI sourceUrl,
        DiscoveryJobStatus status,
        int totalDiscovered,
        int newDocuments,
        int existingDocuments,
        int ignoredDocuments,
        int failedSources,
        String message,
        String recommendation,
        String crawlerTypeUsed,
        boolean sourceReachable,
        boolean htmlFetched,
        int linksScanned,
        int pdfLinksFound,
        String reasonWhenZeroResults,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
