package com.marketmind.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

public record PipelineEvent(
        UUID id,
        UUID pipelineJobId,
        UUID pipelineStageId,
        PipelineEventType eventType,
        String message,
        String details,
        Instant createdAt) {
}
