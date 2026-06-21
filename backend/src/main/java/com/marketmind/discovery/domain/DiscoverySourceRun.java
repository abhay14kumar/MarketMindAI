package com.marketmind.discovery.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DiscoverySourceRun(
        UUID id,
        UUID discoveryJobId,
        DiscoverySourceType sourceType,
        URI sourceUrl,
        DiscoveryJobStatus status,
        int discoveredCount,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
