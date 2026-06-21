package com.marketmind.ai.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.ai.domain.EmbeddingJobStatus;

public record EmbeddingJobResponse(
        UUID id,
        UUID documentId,
        UUID documentVersionId,
        EmbeddingJobStatus status,
        int totalChunks,
        int embeddedChunks,
        int failedChunks,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {
}
