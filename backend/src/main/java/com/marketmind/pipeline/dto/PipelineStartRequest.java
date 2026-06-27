package com.marketmind.pipeline.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Starts orchestration from discovery metadata or an existing document")
public record PipelineStartRequest(
        UUID discoveredDocumentId,
        UUID documentId) {
}
