package com.marketmind.documents.api;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.DownloadJobStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asynchronous document-download job")
public record DownloadJobResponse(
        UUID id,
        UUID documentId,
        UUID sourceId,
        String requestedUrl,
        DownloadJobStatus status,
        int attemptCount,
        int maxAttempts,
        Instant submittedAt,
        Instant startedAt,
        Instant completedAt,
        Instant nextAttemptAt,
        String errorCode,
        String errorMessage) {
}
