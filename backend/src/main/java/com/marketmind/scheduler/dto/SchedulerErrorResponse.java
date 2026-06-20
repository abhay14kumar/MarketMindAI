package com.marketmind.scheduler.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Safe scheduler execution error")
public record SchedulerErrorResponse(
        UUID id,
        UUID schedulerRunId,
        String errorCode,
        String message,
        boolean retryable,
        Instant occurredAt,
        Instant createdAt) {
}
