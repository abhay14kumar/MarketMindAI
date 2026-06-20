package com.marketmind.documents.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.documents.domain.DownloadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "download_job")
class DownloadJobJpaEntity {

    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "requested_url", nullable = false, columnDefinition = "TEXT")
    private String requestedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DownloadStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "retry_of_job_id")
    private UUID retryOfJobId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected DownloadJobJpaEntity() {
    }

    DownloadJobJpaEntity(
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
        this.id = id;
        this.documentId = documentId;
        this.sourceId = sourceId;
        this.requestedUrl = requestedUrl;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.retryOfJobId = retryOfJobId;
        this.submittedAt = submittedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.nextAttemptAt = nextAttemptAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    UUID getId() { return id; }
    UUID getDocumentId() { return documentId; }
    UUID getSourceId() { return sourceId; }
    String getRequestedUrl() { return requestedUrl; }
    DownloadStatus getStatus() { return status; }
    int getAttemptCount() { return attemptCount; }
    int getMaxAttempts() { return maxAttempts; }
    UUID getRetryOfJobId() { return retryOfJobId; }
    Instant getSubmittedAt() { return submittedAt; }
    Instant getStartedAt() { return startedAt; }
    Instant getCompletedAt() { return completedAt; }
    Instant getNextAttemptAt() { return nextAttemptAt; }
    String getErrorCode() { return errorCode; }
    String getErrorMessage() { return errorMessage; }
}
