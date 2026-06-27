package com.marketmind.pipeline.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline.orchestration")
public record PipelineOrchestrationProperties(
        boolean enabled,
        int maxAttempts,
        Duration initialBackoff,
        int summaryMaxCharacters) {

    public PipelineOrchestrationProperties {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException(
                    "pipeline.orchestration.max-attempts must be between 1 and 10.");
        }
        initialBackoff = initialBackoff == null
                ? Duration.ofMillis(250)
                : initialBackoff;
        if (summaryMaxCharacters < 1000) {
            throw new IllegalArgumentException(
                    "pipeline.orchestration.summary-max-characters must be at least 1000.");
        }
    }
}
