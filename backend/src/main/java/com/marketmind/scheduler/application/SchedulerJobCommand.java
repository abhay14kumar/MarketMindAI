package com.marketmind.scheduler.application;

import java.util.Map;

import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerType;

public record SchedulerJobCommand(
        String name,
        String description,
        SchedulerType schedulerType,
        SchedulerJobStatus status,
        String cronExpression,
        String timeZone,
        Map<String, String> configuration) {

    public SchedulerJobCommand {
        configuration = Map.copyOf(configuration);
    }
}
