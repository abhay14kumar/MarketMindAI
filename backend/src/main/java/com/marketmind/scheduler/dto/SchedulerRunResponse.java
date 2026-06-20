package com.marketmind.scheduler.dto;

import java.time.Instant;
import java.util.UUID;

import com.marketmind.scheduler.domain.SchedulerRunStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scheduler execution attempt")
public record SchedulerRunResponse(
        UUID id,
        UUID schedulerJobId,
        SchedulerRunStatus status,
        @Schema(example = "MANUAL") String triggerType,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt,
        long processedItems,
        String correlationId,
        Instant createdAt,
        Instant updatedAt) {
}
