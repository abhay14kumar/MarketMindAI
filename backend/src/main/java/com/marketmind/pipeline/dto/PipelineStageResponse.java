package com.marketmind.pipeline.dto;

import java.time.Instant;
import java.util.UUID;

public record PipelineStageResponse(
        UUID id,
        String stageName,
        String status,
        int attemptCount,
        int maxAttempts,
        long durationMs,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
