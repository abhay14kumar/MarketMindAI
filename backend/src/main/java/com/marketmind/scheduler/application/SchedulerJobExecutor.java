package com.marketmind.scheduler.application;

import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerRunStatus;

public interface SchedulerJobExecutor {

    ExecutionResult execute(SchedulerJob job);

    record ExecutionResult(
            SchedulerRunStatus status,
            String resultSummary,
            String errorMessage,
            long discoveredDocumentsCount,
            long pipelineJobsCreatedCount) {
    }
}
