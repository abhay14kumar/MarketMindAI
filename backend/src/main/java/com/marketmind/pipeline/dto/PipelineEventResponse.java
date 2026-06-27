package com.marketmind.pipeline.dto;

import java.time.Instant;
import java.util.UUID;

public record PipelineEventResponse(
        UUID id,
        UUID pipelineStageId,
        String eventType,
        String message,
        String details,
        Instant createdAt) {
}
