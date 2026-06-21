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
        int failedSources,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
