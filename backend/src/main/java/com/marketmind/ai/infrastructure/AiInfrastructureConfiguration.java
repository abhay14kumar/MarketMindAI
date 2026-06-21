package com.marketmind.ai.infrastructure;

import com.marketmind.ai.application.RagProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    AiInfrastructureProperties.class,
    RagProperties.class
})
public class AiInfrastructureConfiguration {
}
