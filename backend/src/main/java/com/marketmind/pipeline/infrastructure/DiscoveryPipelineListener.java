package com.marketmind.pipeline.infrastructure;

import com.marketmind.discovery.application.DiscoveredDocumentCreatedEvent;
import com.marketmind.pipeline.application.PipelineOrchestrator;
import com.marketmind.pipeline.application.PipelineStartCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DiscoveryPipelineListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DiscoveryPipelineListener.class);

    private final PipelineOrchestrator orchestrator;

    public DiscoveryPipelineListener(PipelineOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiscoveredDocumentCreated(
            DiscoveredDocumentCreatedEvent event) {
        try {
            orchestrator.start(new PipelineStartCommand(
                    event.discoveredDocumentId(), null, null));
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unable to create pipeline job for discovered document {}",
                    event.discoveredDocumentId(),
                    exception);
        }
    }
}
