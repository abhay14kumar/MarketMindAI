package com.marketmind.sources.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "source.validation")
public record SourceValidationProperties(
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int maxPdfTestSizeMb,
        String userAgent) {

    private static final int BYTES_PER_MEGABYTE = 1024 * 1024;

    public SourceValidationProperties {
        if (connectTimeoutSeconds < 1 || readTimeoutSeconds < 1 || maxPdfTestSizeMb < 1) {
            throw new IllegalArgumentException("Source validation limits must be positive.");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("Source validation user agent is required.");
        }
    }

    public Duration connectTimeout() {
        return Duration.ofSeconds(connectTimeoutSeconds);
    }

    public Duration readTimeout() {
        return Duration.ofSeconds(readTimeoutSeconds);
    }

    public long maxPdfTestSizeBytes() {
        return Math.multiplyExact((long) maxPdfTestSizeMb, BYTES_PER_MEGABYTE);
    }
}
