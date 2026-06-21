package com.marketmind.pipeline.application;

import com.marketmind.pipeline.domain.DocumentPipelineRun;

public record PipelineRunSummary(
        DocumentPipelineRun run,
        String documentTitle) {
}
