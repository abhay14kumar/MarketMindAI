package com.marketmind.pipeline.mapper;

import com.marketmind.pipeline.application.PageResult;
import com.marketmind.pipeline.application.PipelineRunDetails;
import com.marketmind.pipeline.domain.DocumentPipelineStep;
import com.marketmind.pipeline.dto.PageResponse;
import com.marketmind.pipeline.dto.PipelineRunResponse;
import com.marketmind.pipeline.dto.PipelineStepResponse;

import org.springframework.stereotype.Component;

@Component
public class DocumentPipelineMapper {

    public PipelineRunResponse toResponse(PipelineRunDetails details) {
        var run = details.summary().run();
        return new PipelineRunResponse(
                run.id(),
                run.documentId(),
                details.summary().documentTitle(),
                run.status().name(),
                run.currentStep().name(),
                run.startedAt(),
                run.completedAt(),
                run.errorMessage(),
                run.createdAt(),
                details.steps().stream().map(this::toResponse).toList());
    }

    public PageResponse<PipelineRunResponse> toResponse(
            PageResult<PipelineRunDetails> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    private PipelineStepResponse toResponse(DocumentPipelineStep step) {
        return new PipelineStepResponse(
                step.id(),
                step.stepName().name(),
                step.status().name(),
                step.startedAt(),
                step.completedAt(),
                step.errorMessage(),
                step.retryCount());
    }
}
