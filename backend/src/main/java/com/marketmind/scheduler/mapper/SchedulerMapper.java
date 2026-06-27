package com.marketmind.scheduler.mapper;

import com.marketmind.scheduler.application.PageResult;
import com.marketmind.scheduler.application.SchedulerJobCommand;
import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerRun;
import com.marketmind.scheduler.dto.PageResponse;
import com.marketmind.scheduler.dto.SchedulerJobRequest;
import com.marketmind.scheduler.dto.SchedulerJobResponse;
import com.marketmind.scheduler.dto.SchedulerRunResponse;

import org.springframework.stereotype.Component;

@Component
public class SchedulerMapper {

    public SchedulerJobCommand toCommand(SchedulerJobRequest request) {
        return new SchedulerJobCommand(
                request.name(),
                request.description(),
                request.schedulerType(),
                request.status(),
                request.cronExpression(),
                request.timeZone(),
                request.configuration());
    }

    public SchedulerJobResponse toResponse(SchedulerJob job) {
        return toResponse(job, null);
    }

    public SchedulerJobResponse toResponse(SchedulerJob job, SchedulerRun latestRun) {
        boolean seeded = Boolean.parseBoolean(job.configuration().getOrDefault("seeded", "false"));
        return new SchedulerJobResponse(
                job.id(),
                job.name(),
                job.description(),
                job.schedulerType(),
                job.status(),
                job.cronExpression(),
                job.timeZone(),
                job.configuration(),
                com.marketmind.scheduler.domain.SchedulerExecutionMode.valueOf(
                        job.configuration().getOrDefault("executionMode", "MOCK")),
                com.marketmind.scheduler.domain.SchedulerImplementationStatus.valueOf(
                        job.configuration().getOrDefault("implementationStatus", "NOT_IMPLEMENTED")),
                seeded && job.nextRunAt() != null,
                seeded && job.lastRunAt() != null,
                job.nextRunAt(),
                job.lastRunAt(),
                latestRun == null ? null : latestRun.status(),
                latestRun == null ? null : firstNonBlank(
                        latestRun.resultSummary(), latestRun.errorMessage()),
                latestRun == null ? null : duration(latestRun),
                latestRun == null ? null : latestRun.startedAt(),
                latestRun == null ? null : latestRun.completedAt(),
                latestRun == null ? 0 : latestRun.discoveredDocumentsCount(),
                latestRun == null ? 0 : latestRun.pipelineJobsCreatedCount(),
                job.createdAt(),
                job.updatedAt());
    }

    public SchedulerRunResponse toResponse(SchedulerRun run) {
        return new SchedulerRunResponse(
                run.id(),
                run.schedulerJobId(),
                run.status(),
                run.triggerType(),
                run.queuedAt(),
                run.startedAt(),
                run.completedAt(),
                duration(run),
                run.processedItems(),
                run.resultSummary(),
                run.errorMessage(),
                run.discoveredDocumentsCount(),
                run.pipelineJobsCreatedCount(),
                run.correlationId(),
                run.createdAt(),
                run.updatedAt());
    }

    public PageResponse<SchedulerJobResponse> toJobPage(PageResult<SchedulerJob> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public PageResponse<SchedulerRunResponse> toRunPage(PageResult<SchedulerRun> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    private long duration(SchedulerRun run) {
        if (run.startedAt() == null || run.completedAt() == null) {
            return 0;
        }
        return Math.max(0, java.time.Duration.between(
                run.startedAt(), run.completedAt()).toMillis());
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
