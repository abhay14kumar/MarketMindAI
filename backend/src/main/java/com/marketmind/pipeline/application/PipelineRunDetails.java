package com.marketmind.pipeline.application;

import java.util.List;

import com.marketmind.pipeline.domain.DocumentPipelineStep;

public record PipelineRunDetails(
        PipelineRunSummary summary,
        List<DocumentPipelineStep> steps) {

    public PipelineRunDetails {
        steps = List.copyOf(steps);
    }
}
