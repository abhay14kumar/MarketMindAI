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
        int failedSources,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
