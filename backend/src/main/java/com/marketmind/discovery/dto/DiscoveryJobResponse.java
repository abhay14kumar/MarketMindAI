package com.marketmind.discovery.dto;

import java.time.Instant;
import java.util.UUID;

public record DiscoveryJobResponse(
        UUID discoveryJobId,
        String sourceType,
        String sourceUrl,
        String status,
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
        Instant completedAt) {
}
