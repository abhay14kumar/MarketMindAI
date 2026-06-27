package com.marketmind.pipeline.infrastructure;

import java.util.UUID;

import com.marketmind.documents.application.DocumentProcessingTrigger;
import com.marketmind.pipeline.application.DocumentPipelineProperties;
import com.marketmind.pipeline.application.PipelineOrchestrator;
import com.marketmind.pipeline.application.PipelineStartCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

@Component
public class AutomatedDocumentProcessingTrigger implements DocumentProcessingTrigger {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AutomatedDocumentProcessingTrigger.class);

    private final PipelineOrchestrator orchestrator;
    private final DocumentPipelineProperties properties;

    public AutomatedDocumentProcessingTrigger(
            @Lazy PipelineOrchestrator orchestrator,
            DocumentPipelineProperties properties) {
        this.orchestrator = orchestrator;
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
            orchestrator.start(new PipelineStartCommand(null, documentId, null));
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Automated processing failed unexpectedly for document {}",
                    documentId,
                    exception);
        }
    }
}
