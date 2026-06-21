package com.marketmind.pipeline.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Execution state for one document pipeline step")
public record PipelineStepResponse(
        UUID id,
        String stepName,
        String status,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        int retryCount) {
}
