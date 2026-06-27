package com.marketmind.sourceintelligence.application;

public record SourceIntelligenceMetrics(
        long totalSources,
        long officialSources,
        long healthySources,
        long degradedSources,
        long enabledConnectors,
        long discoveryJobs,
        long pipelineJobs,
        long documentsDiscovered,
        double averageTrustScore,
        double averageReliability,
        double coveragePercent) {
}
