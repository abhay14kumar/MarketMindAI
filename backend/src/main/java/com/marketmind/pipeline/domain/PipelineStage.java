package com.marketmind.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

public record PipelineStage(
        UUID id,
        UUID pipelineJobId,
        PipelineStageName stageName,
        PipelineStageStatus status,
        int attemptCount,
        int maxAttempts,
        long durationMs,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
