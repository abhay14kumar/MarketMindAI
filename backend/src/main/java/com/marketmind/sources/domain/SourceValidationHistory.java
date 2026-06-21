package com.marketmind.sources.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SourceValidationHistory(
        UUID id,
        UUID sourceId,
        String sourceName,
        ValidationStatus validationStatus,
        boolean reachable,
        Integer httpStatus,
        long latencyMs,
        boolean robotsTxtAvailable,
        Integer robotsTxtStatus,
        CapabilityStatus pdfCapabilityStatus,
        String message,
        Set<CapabilityType> supportedCapabilities,
        Instant validatedAt,
        Instant createdAt) {

    public SourceValidationHistory {
        supportedCapabilities = supportedCapabilities == null
                ? Set.of()
                : Set.copyOf(supportedCapabilities);
    }
}
