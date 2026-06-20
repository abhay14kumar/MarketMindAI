package com.marketmind.sources.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SourceRegistry(
        UUID id,
        String code,
        String name,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        URI baseUrl,
        URI documentationUrl,
        Set<CapabilityType> capabilities,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public SourceRegistry {
        capabilities = Set.copyOf(capabilities);
    }
}
