package com.marketmind.discovery.dto;

import java.time.Instant;
import java.util.UUID;

public record DiscoverySourceRunResponse(
        UUID id,
        String sourceType,
        String sourceUrl,
        String status,
        int discoveredCount,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
