package com.marketmind.pipeline.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Automated document processing pipeline run")
public record PipelineRunResponse(
        UUID id,
        UUID documentId,
        String documentTitle,
        String status,
        String currentStep,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        Instant createdAt,
        List<PipelineStepResponse> steps) {

    public PipelineRunResponse {
        steps = List.copyOf(steps);
    }
}
