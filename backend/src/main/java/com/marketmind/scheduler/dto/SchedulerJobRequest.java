package com.marketmind.scheduler.dto;

import java.util.Map;

import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Scheduler job create or replacement request")
public record SchedulerJobRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull SchedulerType schedulerType,
        @NotNull SchedulerJobStatus status,
        @NotBlank
        @Size(max = 120)
        @Schema(example = "0 0/30 * * * *")
        String cronExpression,
        @NotBlank
        @Size(max = 64)
        @Schema(example = "Asia/Kolkata")
        String timeZone,
        @Size(max = 25) Map<@Size(max = 100) String, @Size(max = 500) String> configuration) {

    public SchedulerJobRequest {
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}
