package com.marketmind.pipeline.infrastructure;

import java.util.concurrent.Executor;

import com.marketmind.pipeline.application.DocumentPipelineProperties;
import com.marketmind.pipeline.application.PipelineOrchestrationProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties({
    DocumentPipelineProperties.class,
    PipelineOrchestrationProperties.class
})
public class PipelineOrchestrationConfiguration {

    @Bean(name = "documentPipelineExecutor")
    public Executor documentPipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("document-pipeline-");
        executor.initialize();
        return executor;
    }
}
