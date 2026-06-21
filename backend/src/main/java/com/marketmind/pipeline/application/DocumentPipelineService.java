package com.marketmind.pipeline.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketmind.ai.application.DocumentEmbeddingService;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;
import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.application.DocumentCatalog;
import com.marketmind.documents.application.PdfTextExtractionService;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.pipeline.domain.DocumentPipelineRun;
import com.marketmind.pipeline.domain.DocumentPipelineStep;
import com.marketmind.pipeline.domain.PipelineStatus;
import com.marketmind.pipeline.domain.PipelineStepName;

import org.springframework.stereotype.Service;

@Service
public class DocumentPipelineService {

    private final DocumentPipelineRepository repository;
    private final DocumentCatalog documentCatalog;
    private final PdfTextExtractionService extractionService;
    private final DocumentEmbeddingService embeddingService;
    private final Clock clock;

    public DocumentPipelineService(
            DocumentPipelineRepository repository,
            DocumentCatalog documentCatalog,
            PdfTextExtractionService extractionService,
            DocumentEmbeddingService embeddingService,
            Clock clock) {
        this.repository = repository;
        this.documentCatalog = documentCatalog;
        this.extractionService = extractionService;
        this.embeddingService = embeddingService;
        this.clock = clock;
    }

    public DocumentPipelineRun processDownloaded(UUID documentId) {
        requireDocument(documentId);
        DocumentPipelineRun run = startRun(documentId, PipelineStepName.DOWNLOAD);
        completeStep(startStep(run, PipelineStepName.DOWNLOAD));
        return executeFrom(run, PipelineStepName.TEXT_EXTRACTION);
    }

    public DocumentPipelineRun retry(UUID documentId) {
        requireDocument(documentId);
        PipelineRunDetails latest = getByDocumentId(documentId);
        if (latest.summary().run().status() == PipelineStatus.COMPLETED) {
            throw new ConflictException("The document pipeline has already completed.");
        }

        PipelineStepName retryFrom = latest.steps().stream()
                .filter(step -> step.status() == PipelineStatus.FAILED
                        || step.status() == PipelineStatus.PARTIAL)
                .map(DocumentPipelineStep::stepName)
                .findFirst()
                .map(step -> step == PipelineStepName.EMBEDDING
                        ? PipelineStepName.CHUNKING
                        : step)
                .orElse(PipelineStepName.TEXT_EXTRACTION);

        DocumentPipelineRun run = startRun(documentId, retryFrom);
        for (PipelineStepName step : PipelineStepName.values()) {
            if (step.ordinal() >= retryFrom.ordinal()) {
                break;
            }
            skipStep(run, step, "Completed in an earlier pipeline run.");
        }
        return executeFrom(run, retryFrom);
    }

    public PageResult<PipelineRunDetails> getRuns(int page, int size) {
        PageResult<PipelineRunSummary> summaries = repository.findRuns(page, size);
        List<PipelineRunDetails> details = summaries.content().stream()
                .map(summary -> new PipelineRunDetails(
                        summary,
                        repository.findSteps(summary.run().id())))
                .toList();
        return new PageResult<>(
                details,
                summaries.page(),
                summaries.size(),
                summaries.totalElements(),
                summaries.totalPages());
    }

    public PipelineRunDetails getRun(UUID runId) {
        PipelineRunSummary summary = repository.findRun(runId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document pipeline run not found: " + runId));
        return new PipelineRunDetails(summary, repository.findSteps(runId));
    }

    public PipelineRunDetails getByDocumentId(UUID documentId) {
        requireDocument(documentId);
        PipelineRunSummary summary = repository.findLatestRun(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pipeline run exists for document: " + documentId));
        return new PipelineRunDetails(
                summary,
                repository.findSteps(summary.run().id()));
    }

    private DocumentPipelineRun executeFrom(
            DocumentPipelineRun run,
            PipelineStepName startAt) {
        if (startAt.ordinal() <= PipelineStepName.TEXT_EXTRACTION.ordinal()) {
            run = updateCurrentStep(run, PipelineStepName.TEXT_EXTRACTION);
            DocumentPipelineStep extractionStep =
                    startStep(run, PipelineStepName.TEXT_EXTRACTION);
            try {
                DocumentTextExtraction extraction =
                        extractionService.extract(run.documentId());
                if (extraction.extractionStatus() != ExtractionStatus.COMPLETED) {
                    failStep(extractionStep, extraction.errorMessage());
                    skipRemaining(run, PipelineStepName.CHUNKING);
                    return finishRun(
                            run,
                            PipelineStatus.FAILED,
                            PipelineStepName.TEXT_EXTRACTION,
                            extraction.errorMessage());
                }
                completeStep(extractionStep);
            } catch (RuntimeException exception) {
                failStep(extractionStep, safeMessage(exception));
                skipRemaining(run, PipelineStepName.CHUNKING);
                return finishRun(
                        run,
                        PipelineStatus.FAILED,
                        PipelineStepName.TEXT_EXTRACTION,
                        safeMessage(exception));
            }
        }

        run = updateCurrentStep(run, PipelineStepName.CHUNKING);
        DocumentPipelineStep chunkingStep = startStep(run, PipelineStepName.CHUNKING);
        DocumentPipelineStep embeddingStep = null;
        try {
            embeddingStep = startStep(run, PipelineStepName.EMBEDDING);
            DocumentEmbeddingJob job = embeddingService.embed(run.documentId());
            if (job.totalChunks() == 0) {
                failStep(chunkingStep, job.errorMessage());
                finishStep(
                        embeddingStep,
                        PipelineStatus.SKIPPED,
                        "No chunks were available for embedding.");
                skipStep(run, PipelineStepName.AI_READY, "Chunking did not complete.");
                return finishRun(
                        run,
                        PipelineStatus.FAILED,
                        PipelineStepName.CHUNKING,
                        job.errorMessage());
            }
            completeStep(chunkingStep);
            run = updateCurrentStep(run, PipelineStepName.EMBEDDING);

            if (job.status() == EmbeddingJobStatus.COMPLETED) {
                completeStep(embeddingStep);
                return markAiReady(run);
            }

            PipelineStatus status = job.status() == EmbeddingJobStatus.PARTIAL
                    ? PipelineStatus.PARTIAL
                    : PipelineStatus.FAILED;
            finishStep(embeddingStep, status, job.errorMessage());
            skipStep(run, PipelineStepName.AI_READY, "Embedding did not complete.");
            return finishRun(
                    run, status, PipelineStepName.EMBEDDING, job.errorMessage());
        } catch (RuntimeException exception) {
            failStep(chunkingStep, safeMessage(exception));
            if (embeddingStep != null) {
                failStep(embeddingStep, safeMessage(exception));
            }
            skipStep(run, PipelineStepName.AI_READY, "Embedding did not complete.");
            return finishRun(
                    run,
                    PipelineStatus.FAILED,
                    PipelineStepName.EMBEDDING,
                    safeMessage(exception));
        }
    }

    private DocumentPipelineRun markAiReady(DocumentPipelineRun run) {
        run = updateCurrentStep(run, PipelineStepName.AI_READY);
        DocumentPipelineStep readyStep = startStep(run, PipelineStepName.AI_READY);
        try {
            Document document = requireDocument(run.documentId());
            documentCatalog.saveDocument(new Document(
                    document.id(),
                    document.companyId(),
                    document.source(),
                    document.documentType(),
                    document.title(),
                    document.sourceUrl(),
                    document.publicationDate(),
                    document.reportingPeriod(),
                    document.fiscalYear(),
                    document.quarter(),
                    DocumentStatus.AI_READY,
                    document.currentVersionId(),
                    document.createdAt(),
                    clock.instant()));
            completeStep(readyStep);
            return finishRun(
                    run, PipelineStatus.COMPLETED, PipelineStepName.AI_READY, null);
        } catch (RuntimeException exception) {
            failStep(readyStep, safeMessage(exception));
            return finishRun(
                    run,
                    PipelineStatus.FAILED,
                    PipelineStepName.AI_READY,
                    safeMessage(exception));
        }
    }

    private DocumentPipelineRun startRun(UUID documentId, PipelineStepName currentStep) {
        Instant now = clock.instant();
        return repository.saveRun(new DocumentPipelineRun(
                UUID.randomUUID(),
                documentId,
                PipelineStatus.STARTED,
                currentStep,
                now,
                null,
                null,
                now));
    }

    private DocumentPipelineRun updateCurrentStep(
            DocumentPipelineRun run,
            PipelineStepName currentStep) {
        return repository.saveRun(new DocumentPipelineRun(
                run.id(),
                run.documentId(),
                PipelineStatus.STARTED,
                currentStep,
                run.startedAt(),
                null,
                null,
                run.createdAt()));
    }

    private DocumentPipelineRun finishRun(
            DocumentPipelineRun run,
            PipelineStatus status,
            PipelineStepName currentStep,
            String errorMessage) {
        return repository.saveRun(new DocumentPipelineRun(
                run.id(),
                run.documentId(),
                status,
                currentStep,
                run.startedAt(),
                clock.instant(),
                truncate(errorMessage),
                run.createdAt()));
    }

    private DocumentPipelineStep startStep(
            DocumentPipelineRun run,
            PipelineStepName stepName) {
        Instant now = clock.instant();
        return repository.saveStep(new DocumentPipelineStep(
                UUID.randomUUID(),
                run.id(),
                run.documentId(),
                stepName,
                PipelineStatus.STARTED,
                now,
                null,
                null,
                repository.nextRetryCount(run.documentId(), stepName),
                now));
    }

    private void completeStep(DocumentPipelineStep step) {
        finishStep(step, PipelineStatus.COMPLETED, null);
    }

    private void failStep(DocumentPipelineStep step, String errorMessage) {
        finishStep(step, PipelineStatus.FAILED, errorMessage);
    }

    private void finishStep(
            DocumentPipelineStep step,
            PipelineStatus status,
            String errorMessage) {
        repository.saveStep(new DocumentPipelineStep(
                step.id(),
                step.pipelineRunId(),
                step.documentId(),
                step.stepName(),
                status,
                step.startedAt(),
                clock.instant(),
                truncate(errorMessage),
                step.retryCount(),
                step.createdAt()));
    }

    private void skipStep(
            DocumentPipelineRun run,
            PipelineStepName stepName,
            String reason) {
        Instant now = clock.instant();
        repository.saveStep(new DocumentPipelineStep(
                UUID.randomUUID(),
                run.id(),
                run.documentId(),
                stepName,
                PipelineStatus.SKIPPED,
                now,
                now,
                reason,
                repository.nextRetryCount(run.documentId(), stepName),
                now));
    }

    private void skipRemaining(
            DocumentPipelineRun run,
            PipelineStepName firstStep) {
        for (PipelineStepName step : PipelineStepName.values()) {
            if (step.ordinal() >= firstStep.ordinal()) {
                skipStep(run, step, "A prerequisite pipeline step failed.");
            }
        }
    }

    private Document requireDocument(UUID documentId) {
        return documentCatalog.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));
    }

    private String safeMessage(Throwable exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private String truncate(String value) {
        return value == null || value.length() <= 2000
                ? value
                : value.substring(0, 2000);
    }
}
