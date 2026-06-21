package com.marketmind.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentPipelineStep(
        UUID id,
        UUID pipelineRunId,
        UUID documentId,
        PipelineStepName stepName,
        PipelineStatus status,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        int retryCount,
        Instant createdAt) {
}
