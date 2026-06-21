package com.marketmind.pipeline.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pipeline.document")
public record DocumentPipelineProperties(boolean autoProcessEnabled) {
}
