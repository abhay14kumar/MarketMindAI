package com.marketmind.scheduler.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configured scheduler job")
public record SchedulerJobResponse(
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

    public SchedulerJobResponse {
        configuration = Map.copyOf(configuration);
    }
}
