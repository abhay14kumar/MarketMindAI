package com.marketmind.documents.infrastructure;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.marketmind.documents.application.DocumentDownloadPolicy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "document.download")
public record DocumentDownloadProperties(
        @Min(1) @Max(300) int timeoutSeconds,
        @Min(1) @Max(1024) int maxFileSizeMb) implements DocumentDownloadPolicy {

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    public long maxFileSizeBytes() {
        return Math.multiplyExact((long) maxFileSizeMb, 1024L * 1024L);
    }
}
