package com.marketmind.pipeline.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.pipeline.domain.DocumentPipelineRun;
import com.marketmind.pipeline.domain.DocumentPipelineStep;
import com.marketmind.pipeline.domain.PipelineStepName;

public interface DocumentPipelineRepository {

    DocumentPipelineRun saveRun(DocumentPipelineRun run);

    DocumentPipelineStep saveStep(DocumentPipelineStep step);

    PageResult<PipelineRunSummary> findRuns(int page, int size);

    Optional<PipelineRunSummary> findRun(UUID runId);

    Optional<PipelineRunSummary> findLatestRun(UUID documentId);

    List<DocumentPipelineStep> findSteps(UUID runId);

    int nextRetryCount(UUID documentId, PipelineStepName stepName);
}
