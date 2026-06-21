package com.marketmind.discovery.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discovery")
public record DiscoveryProperties(
        Duration timeout,
        int maxHtmlBytes,
        String userAgent) {

    public DiscoveryProperties {
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        if (maxHtmlBytes < 1024) {
            throw new IllegalArgumentException(
                    "discovery.max-html-bytes must be at least 1024.");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("discovery.user-agent is required.");
        }
    }
}
