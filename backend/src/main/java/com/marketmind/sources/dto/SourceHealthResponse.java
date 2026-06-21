package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.CapabilityStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Source health observation")
public record SourceHealthResponse(
        UUID id,
        UUID sourceId,
        SourceStatus status,
        boolean available,
        long latencyMs,
        String message,
        Instant checkedAt,
        Integer lastHttpStatus,
        Long lastLatencyMs,
        Boolean robotsTxtAvailable,
        Integer robotsTxtStatus,
        CapabilityStatus pdfCapabilityStatus,
        Instant lastValidatedAt) {
}
