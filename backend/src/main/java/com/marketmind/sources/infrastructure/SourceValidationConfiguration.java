package com.marketmind.sources.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SourceValidationProperties.class)
public class SourceValidationConfiguration {
}
