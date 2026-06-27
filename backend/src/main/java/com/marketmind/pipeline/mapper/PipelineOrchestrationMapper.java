package com.marketmind.pipeline.mapper;

import java.util.List;

import com.marketmind.pipeline.application.PageResult;
import com.marketmind.pipeline.application.PipelineJobDetails;
import com.marketmind.pipeline.application.PipelineMetrics;
import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineStage;
import com.marketmind.pipeline.dto.PageResponse;
import com.marketmind.pipeline.dto.PipelineEventResponse;
import com.marketmind.pipeline.dto.PipelineJobResponse;
import com.marketmind.pipeline.dto.PipelineMetricsResponse;
import com.marketmind.pipeline.dto.PipelineStageResponse;

import org.springframework.stereotype.Component;

@Component
public class PipelineOrchestrationMapper {

    public PipelineJobResponse toResponse(PipelineJob job) {
        return toResponse(job, List.of(), List.of());
    }

    public PipelineJobResponse toResponse(PipelineJobDetails details) {
        return toResponse(details.job(), details.stages(), details.events());
    }

    public PageResponse<PipelineJobResponse> toResponse(PageResult<PipelineJob> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public PipelineEventResponse toResponse(PipelineEvent event) {
        return new PipelineEventResponse(
                event.id(), event.pipelineStageId(), event.eventType().name(),
                event.message(), event.details(), event.createdAt());
    }

    public PipelineMetricsResponse toResponse(PipelineMetrics metrics) {
        return new PipelineMetricsResponse(
                metrics.totalJobs(), metrics.runningJobs(),
                metrics.completedJobs(), metrics.failedJobs(),
                metrics.successRate(), metrics.averageDurationMs());
    }

    private PipelineJobResponse toResponse(
            PipelineJob job,
            List<PipelineStage> stages,
            List<PipelineEvent> events) {
        return new PipelineJobResponse(
                job.id(), job.discoveredDocumentId(), job.documentId(),
                job.correlationId(), job.status().name(),
                job.currentStage() == null ? null : job.currentStage().name(),
                job.progressPercent(), job.errorMessage(), job.startedAt(),
                job.completedAt(), job.createdAt(), job.updatedAt(),
                stages.stream().map(this::toResponse).toList(),
                events.stream().map(this::toResponse).toList());
    }

    private PipelineStageResponse toResponse(PipelineStage stage) {
        return new PipelineStageResponse(
                stage.id(), stage.stageName().name(), stage.status().name(),
                stage.attemptCount(), stage.maxAttempts(), stage.durationMs(),
                stage.errorMessage(), stage.startedAt(), stage.completedAt());
    }
}
