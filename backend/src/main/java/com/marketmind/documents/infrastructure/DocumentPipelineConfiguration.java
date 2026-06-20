package com.marketmind.documents.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    DocumentStorageProperties.class,
    DocumentDownloadProperties.class
})
public class DocumentPipelineConfiguration {
}
