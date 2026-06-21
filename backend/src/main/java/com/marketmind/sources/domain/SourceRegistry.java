package com.marketmind.sources.domain;

import java.net.URI;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SourceRegistry(
        UUID id,
        String code,
        String name,
        String organization,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        URI baseUrl,
        URI robotsUrl,
        URI documentationUrl,
        URI samplePdfUrl,
        Set<CapabilityType> capabilities,
        boolean enabled,
        int priority,
        BigDecimal reliabilityScore,
        Instant createdAt,
        Instant updatedAt) {

    public SourceRegistry {
        capabilities = Set.copyOf(capabilities);
    }
}
