package com.marketmind.pipeline.dto;

public record PipelineMetricsResponse(
        long totalJobs,
        long runningJobs,
        long completedJobs,
        long failedJobs,
        double successRate,
        double averageDurationMs) {
}
