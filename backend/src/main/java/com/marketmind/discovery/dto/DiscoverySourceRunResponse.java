package com.marketmind.discovery.dto;

import java.time.Instant;
import java.util.UUID;

public record DiscoverySourceRunResponse(
        UUID id,
        String sourceType,
        String sourceUrl,
        String status,
        int discoveredCount,
        String crawlerType,
        Integer httpStatus,
        long fetchedHtmlBytes,
        int totalLinksFound,
        int pdfLinksFound,
        int skippedLinksCount,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
