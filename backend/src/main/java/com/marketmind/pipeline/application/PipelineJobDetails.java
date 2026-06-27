package com.marketmind.pipeline.application;

import java.util.List;

import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineStage;

public record PipelineJobDetails(
        PipelineJob job,
        List<PipelineStage> stages,
        List<PipelineEvent> events) {

    public PipelineJobDetails {
        stages = List.copyOf(stages);
        events = List.copyOf(events);
    }
}
