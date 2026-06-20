package com.marketmind.documents.scheduler;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documents.scheduler")
public record DocumentSchedulerProperties(
        Duration fixedDelay,
        Duration initialDelay) {
}
