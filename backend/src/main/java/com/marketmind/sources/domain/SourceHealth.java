package com.marketmind.sources.domain;

import java.time.Instant;
import java.util.UUID;

public record SourceHealth(
        UUID id,
        UUID sourceId,
        SourceStatus status,
        boolean available,
        long latencyMs,
        String message,
        Instant checkedAt,
        Instant createdAt) {
}
