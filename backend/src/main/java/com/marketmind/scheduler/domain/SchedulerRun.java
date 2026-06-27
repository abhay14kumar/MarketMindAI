package com.marketmind.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

public record SchedulerRun(
        UUID id,
        UUID schedulerJobId,
        SchedulerRunStatus status,
        String triggerType,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt,
        long processedItems,
        String resultSummary,
        String errorMessage,
        long discoveredDocumentsCount,
        long pipelineJobsCreatedCount,
        String correlationId,
        Instant createdAt,
        Instant updatedAt) {
}
