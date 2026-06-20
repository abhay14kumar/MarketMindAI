package com.marketmind.documents.domain;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DownloadJob(
        UUID id,
        UUID documentId,
        UUID sourceId,
        URI requestedUrl,
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
