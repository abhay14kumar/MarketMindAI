package com.marketmind.documents.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DocumentSchedulerProperties.class)
@ConditionalOnProperty(
        name = "app.documents.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DocumentSchedulerConfiguration {
}
