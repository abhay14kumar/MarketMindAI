package com.marketmind.pipeline.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineStage;

public interface PipelineJobRepository {

    PipelineJob saveJob(PipelineJob job);

    PipelineStage saveStage(PipelineStage stage);

    PipelineEvent saveEvent(PipelineEvent event);

    Optional<PipelineJob> findJob(UUID jobId);

    PageResult<PipelineJob> findJobs(int page, int size);

    List<PipelineStage> findStages(UUID jobId);

    List<PipelineEvent> findEvents(UUID jobId);

    PipelineMetrics metrics();
}
