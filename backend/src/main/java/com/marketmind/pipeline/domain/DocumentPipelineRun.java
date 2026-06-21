package com.marketmind.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentPipelineRun(
        UUID id,
        UUID documentId,
        PipelineStatus status,
        PipelineStepName currentStep,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        Instant createdAt) {
}
