package com.marketmind.documents.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DownloadJob(
        UUID id,
        UUID documentId,
        UUID sourceId,
        URI requestedUrl,
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
