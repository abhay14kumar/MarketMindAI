package com.marketmind.scheduler.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SchedulerJob(
        UUID id,
        String name,
        String description,
        SchedulerType schedulerType,
        SchedulerJobStatus status,
        String cronExpression,
        String timeZone,
        Map<String, String> configuration,
        Instant nextRunAt,
        Instant lastRunAt,
        Instant createdAt,
        Instant updatedAt) {

    public SchedulerJob {
        configuration = Map.copyOf(configuration);
    }
}
