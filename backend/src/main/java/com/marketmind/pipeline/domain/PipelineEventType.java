package com.marketmind.pipeline.domain;

public enum PipelineEventType {
    JOB_CREATED,
    JOB_STARTED,
    STAGE_STARTED,
    STAGE_RETRYING,
    STAGE_COMPLETED,
    STAGE_FAILED,
    STAGE_SKIPPED,
    SUMMARY_GENERATED,
    JOB_COMPLETED,
    JOB_FAILED
}
