package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered financial data source without credential material")
public record SourceRegistryResponse(
        UUID id,
        String code,
        String name,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        String baseUrl,
        String documentationUrl,
        Set<CapabilityType> capabilities,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public SourceRegistryResponse {
        capabilities = Set.copyOf(capabilities);
    }
}
