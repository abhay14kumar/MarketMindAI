package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.sources.domain.CapabilityType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Source capability support")
public record SourceCapabilityResponse(
        UUID id,
        UUID sourceId,
        CapabilityType capabilityType,
        boolean supported,
        Instant verifiedAt) {
}
