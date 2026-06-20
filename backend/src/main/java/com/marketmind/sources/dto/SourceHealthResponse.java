package com.marketmind.sources.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.sources.domain.SourceStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Latest mock source health observation")
public record SourceHealthResponse(
        UUID id,
        UUID sourceId,
        SourceStatus status,
        boolean available,
        long latencyMs,
        String message,
        Instant checkedAt) {
}
