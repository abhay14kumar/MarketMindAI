package com.marketmind.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

public record SchedulerError(
        UUID id,
        UUID schedulerRunId,
        String errorCode,
        String message,
        String details,
        boolean retryable,
        Instant occurredAt,
        Instant createdAt) {
}
