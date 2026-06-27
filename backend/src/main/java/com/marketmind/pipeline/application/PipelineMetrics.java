package com.marketmind.pipeline.application;

public record PipelineMetrics(
        long totalJobs,
        long runningJobs,
        long completedJobs,
        long failedJobs,
        double successRate,
        double averageDurationMs) {
}
