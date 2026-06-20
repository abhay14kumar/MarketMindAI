package com.marketmind.sources.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SourceValidationHistory(
        UUID id,
        UUID sourceId,
        ValidationStatus validationStatus,
        boolean available,
        long latencyMs,
        String message,
        Set<CapabilityType> supportedCapabilities,
        Instant validatedAt,
        Instant createdAt) {

    public SourceValidationHistory {
        supportedCapabilities = Set.copyOf(supportedCapabilities);
    }
}
