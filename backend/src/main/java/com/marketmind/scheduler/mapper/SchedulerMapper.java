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
        return new SchedulerJobResponse(
                job.id(),
                job.name(),
                job.description(),
                job.schedulerType(),
                job.status(),
                job.cronExpression(),
                job.timeZone(),
                job.configuration(),
                job.nextRunAt(),
                job.lastRunAt(),
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
                run.processedItems(),
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
}
