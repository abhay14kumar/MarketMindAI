package com.marketmind.sources.dto;

import java.time.Instant;
import java.math.BigDecimal;
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
        String organization,
        String description,
        SourceType sourceType,
        SourceStatus status,
        AuthenticationType authenticationType,
        RefreshFrequency refreshFrequency,
        String baseUrl,
        String robotsUrl,
        String documentationUrl,
        String samplePdfUrl,
        Set<CapabilityType> capabilities,
        boolean enabled,
        int priority,
        BigDecimal reliabilityScore,
        Instant createdAt,
        Instant updatedAt) {

    public SourceRegistryResponse {
        capabilities = Set.copyOf(capabilities);
    }
}
