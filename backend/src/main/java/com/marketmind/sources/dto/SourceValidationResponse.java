package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.ValidationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mock source validation result")
public record SourceValidationResponse(
        UUID id,
        UUID sourceId,
        ValidationStatus validationStatus,
        boolean available,
        long latencyMs,
        String message,
        Set<CapabilityType> supportedCapabilities,
        Instant validatedAt) {

    public SourceValidationResponse {
        supportedCapabilities = Set.copyOf(supportedCapabilities);
    }
}
