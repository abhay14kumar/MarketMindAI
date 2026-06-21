package com.marketmind.pipeline.infrastructure;

import java.util.UUID;

import com.marketmind.documents.application.DocumentProcessingTrigger;
import com.marketmind.pipeline.application.DocumentPipelineProperties;
import com.marketmind.pipeline.application.DocumentPipelineService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AutomatedDocumentProcessingTrigger implements DocumentProcessingTrigger {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AutomatedDocumentProcessingTrigger.class);

    private final DocumentPipelineService pipelineService;
    private final DocumentPipelineProperties properties;

    public AutomatedDocumentProcessingTrigger(
            DocumentPipelineService pipelineService,
            DocumentPipelineProperties properties) {
        this.pipelineService = pipelineService;
        this.properties = properties;
    }

    @Override
    @Async("documentPipelineExecutor")
    public void documentDownloaded(UUID documentId) {
        if (!properties.autoProcessEnabled()) {
            LOGGER.debug("Automated processing is disabled for document {}", documentId);
            return;
        }
        try {
            pipelineService.processDownloaded(documentId);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Automated processing failed unexpectedly for document {}",
                    documentId,
                    exception);
        }
    }
}
