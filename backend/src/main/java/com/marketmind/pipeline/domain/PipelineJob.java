package com.marketmind.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

public record PipelineJob(
        UUID id,
        UUID discoveredDocumentId,
        UUID documentId,
        String correlationId,
        PipelineJobStatus status,
        PipelineStageName currentStage,
        int progressPercent,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
