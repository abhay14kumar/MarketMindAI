package com.marketmind.pipeline.application;

import java.util.UUID;

public record PipelineStartCommand(
        UUID discoveredDocumentId,
        UUID documentId,
        String correlationId) {
}
