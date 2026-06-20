package com.marketmind.documents.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.DownloadStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asynchronous document acquisition job")
public record DownloadJobResponse(
        UUID id,
        UUID documentId,
        UUID sourceId,
        String requestedUrl,
        DownloadStatus status,
        int attemptCount,
        int maxAttempts,
        UUID retryOfJobId,
        Instant submittedAt,
        Instant startedAt,
        Instant completedAt,
        Instant nextAttemptAt,
        String errorCode,
        String errorMessage) {
}
