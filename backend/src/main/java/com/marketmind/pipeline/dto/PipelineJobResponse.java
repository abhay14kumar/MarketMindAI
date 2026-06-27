package com.marketmind.pipeline.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PipelineJobResponse(
        UUID id,
        UUID discoveredDocumentId,
        UUID documentId,
        String correlationId,
        String status,
        String currentStage,
        int progressPercent,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<PipelineStageResponse> stages,
        List<PipelineEventResponse> events) {

    public PipelineJobResponse {
        stages = List.copyOf(stages);
        events = List.copyOf(events);
    }
}
