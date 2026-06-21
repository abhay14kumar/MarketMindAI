package com.marketmind.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentEmbeddingJob(
        UUID id,
        UUID documentId,
        UUID documentVersionId,
        EmbeddingJobStatus status,
        int totalChunks,
        int embeddedChunks,
        int failedChunks,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
