package com.marketmind.pipeline.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.marketmind.ai.application.ChatClient;
import com.marketmind.ai.application.DocumentEmbeddingService;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;
import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.common.observability.CorrelationIdFilter;
import com.marketmind.discovery.application.DiscoveryRepository;
import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.documents.application.DocumentCatalog;
import com.marketmind.documents.application.DocumentDownloadResult;
import com.marketmind.documents.application.DocumentDownloadService;
import com.marketmind.documents.application.DocumentTextExtractionRepository;
import com.marketmind.documents.application.DownloadDocumentCommand;
import com.marketmind.documents.application.PdfTextExtractionService;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineEventType;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineJobStatus;
import com.marketmind.pipeline.domain.PipelineStage;
import com.marketmind.pipeline.domain.PipelineStageName;
import com.marketmind.pipeline.domain.PipelineStageStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PipelineOrchestrator {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PipelineOrchestrator.class);
    private static final PipelineStageName[] STAGES = PipelineStageName.values();

    private final PipelineJobRepository repository;
    private final DiscoveryRepository discoveryRepository;
    private final DocumentCatalog documentCatalog;
    private final DocumentDownloadService downloadService;
    private final PdfTextExtractionService extractionService;
    private final DocumentTextExtractionRepository extractionRepository;
    private final DocumentEmbeddingService embeddingService;
    private final ChatClient chatClient;
    private final PipelineOrchestrationProperties properties;
    private final PipelineSleeper sleeper;
    private final Executor executor;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public PipelineOrchestrator(
            PipelineJobRepository repository,
            DiscoveryRepository discoveryRepository,
            DocumentCatalog documentCatalog,
            DocumentDownloadService downloadService,
            PdfTextExtractionService extractionService,
            DocumentTextExtractionRepository extractionRepository,
            DocumentEmbeddingService embeddingService,
            ChatClient chatClient,
            PipelineOrchestrationProperties properties,
            PipelineSleeper sleeper,
            @Qualifier("documentPipelineExecutor") Executor executor,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.repository = repository;
        this.discoveryRepository = discoveryRepository;
        this.documentCatalog = documentCatalog;
        this.downloadService = downloadService;
        this.extractionService = extractionService;
        this.extractionRepository = extractionRepository;
        this.embeddingService = embeddingService;
        this.chatClient = chatClient;
        this.properties = properties;
        this.sleeper = sleeper;
        this.executor = executor;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public PipelineJob start(PipelineStartCommand command) {
        if (!properties.enabled()) {
            throw new ConflictException("Pipeline orchestration is disabled.");
        }
        validateStart(command);
        Instant now = clock.instant();
        PipelineJob job = repository.saveJob(new PipelineJob(
                UUID.randomUUID(),
                command.discoveredDocumentId(),
                command.documentId(),
                correlationId(command.correlationId()),
                PipelineJobStatus.QUEUED,
                command.documentId() == null
                        ? PipelineStageName.DISCOVERY
                        : PipelineStageName.TEXT_EXTRACTION,
                0,
                null,
                null,
                null,
                now,
                now));
        initializeStages(job);
        event(job, null, PipelineEventType.JOB_CREATED, "Pipeline job created.", null);
        counter("marketmind.pipeline.jobs.started").increment();
        executor.execute(() -> execute(job.id()));
        return job;
    }

    public PipelineJob retry(UUID jobId, String correlationId) {
        PipelineJob failed = getJob(jobId).job();
        if (failed.status() != PipelineJobStatus.FAILED
                && failed.status() != PipelineJobStatus.PARTIAL) {
            throw new ConflictException("Only failed or partial pipeline jobs can be retried.");
        }
        return start(new PipelineStartCommand(
                failed.discoveredDocumentId(),
                failed.documentId(),
                correlationId));
    }

    public PageResult<PipelineJob> getJobs(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        return repository.findJobs(page, size);
    }

    public PipelineJobDetails getJob(UUID jobId) {
        PipelineJob job = repository.findJob(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pipeline job not found: " + jobId));
        return new PipelineJobDetails(
                job,
                repository.findStages(jobId),
                repository.findEvents(jobId));
    }

    public List<PipelineEvent> getEvents(UUID jobId) {
        getJob(jobId);
        return repository.findEvents(jobId);
    }

    public PipelineMetrics getMetrics() {
        return repository.metrics();
    }

    void execute(UUID jobId) {
        PipelineJob initial = getJob(jobId).job();
        MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID, initial.correlationId());
        MDC.put(CorrelationIdFilter.MDC_REQUEST_ID, initial.correlationId());
        Instant jobStarted = clock.instant();
        ExecutionContext context = new ExecutionContext(
                initial,
                initial.documentId(),
                initial.discoveredDocumentId());
        try {
            context.job = saveJob(context.job, PipelineJobStatus.RUNNING,
                    context.job.currentStage(), 0, null, jobStarted, null);
            event(context.job, null, PipelineEventType.JOB_STARTED,
                    "Pipeline execution started.", null);

            if (context.documentId == null) {
                runStage(context, PipelineStageName.DISCOVERY, () -> {
                    context.discovered = discoveryRepository
                            .findDocument(context.discoveredDocumentId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Discovered document not found: "
                                            + context.discoveredDocumentId));
                    if (context.discovered.status() == DiscoveredDocumentStatus.IGNORED) {
                        throw new IllegalStateException(
                                "Ignored documents cannot enter the pipeline.");
                    }
                    return null;
                });
                runStage(context, PipelineStageName.DOWNLOAD, () -> {
                    DocumentDownloadResult result =
                            downloadService.downloadWithoutProcessing(
                                    downloadCommand(context.discovered));
                    context.documentId = result.document().id();
                    context.job = saveJob(context.job, PipelineJobStatus.RUNNING,
                            PipelineStageName.DOWNLOAD, progress(PipelineStageName.DOWNLOAD),
                            null, jobStarted, null, context.documentId);
                    markDiscoveredIngested(context.discovered);
                    return null;
                });
            } else {
                skipStage(context, PipelineStageName.DISCOVERY,
                        "Pipeline started from an existing document.");
                skipStage(context, PipelineStageName.DOWNLOAD,
                        "Document is already downloaded.");
            }

            runStage(context, PipelineStageName.TEXT_EXTRACTION, () -> {
                DocumentTextExtraction extraction =
                        extractionService.extract(context.documentId);
                if (extraction.extractionStatus() != ExtractionStatus.COMPLETED) {
                    throw new IllegalStateException(
                            extraction.errorMessage() == null
                                    ? "Text extraction did not complete."
                                    : extraction.errorMessage());
                }
                return null;
            });

            final DocumentEmbeddingJob[] embeddingResult = new DocumentEmbeddingJob[1];
            runStage(context, PipelineStageName.CHUNKING, () -> {
                embeddingResult[0] = embeddingService.embed(context.documentId);
                if (embeddingResult[0].totalChunks() == 0) {
                    throw new IllegalStateException(
                            "No text chunks were produced.");
                }
                return null;
            });
            runDerivedStage(context, PipelineStageName.EMBEDDING, () -> {
                if (embeddingResult[0].status() == EmbeddingJobStatus.FAILED) {
                    throw new IllegalStateException(
                            safe(embeddingResult[0].errorMessage(),
                                    "Embedding failed."));
                }
            });
            runDerivedStage(context, PipelineStageName.QDRANT_INDEXING, () -> {
                if (embeddingResult[0].status() != EmbeddingJobStatus.COMPLETED) {
                    throw new IllegalStateException(
                            safe(embeddingResult[0].errorMessage(),
                                    "Qdrant indexing was incomplete."));
                }
            });

            runStage(context, PipelineStageName.AI_SUMMARY, () -> {
                DocumentTextExtraction extraction = extractionRepository
                        .findLatestByDocumentId(context.documentId)
                        .filter(value -> value.extractionStatus()
                                == ExtractionStatus.COMPLETED)
                        .orElseThrow(() -> new IllegalStateException(
                                "Completed extraction is unavailable for summarization."));
                String text = extraction.extractedText();
                String bounded = text.substring(
                        0, Math.min(text.length(), properties.summaryMaxCharacters()));
                String summary = chatClient.answer(
                        "Summarize this document in concise factual bullet points.",
                        bounded);
                event(context.job, null, PipelineEventType.SUMMARY_GENERATED,
                        "AI summary generated.", summary);
                return null;
            });

            runStage(context, PipelineStageName.AI_READY, () -> {
                Document document = documentCatalog.findDocumentById(context.documentId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Document not found: " + context.documentId));
                documentCatalog.saveDocument(withStatus(document, DocumentStatus.AI_READY));
                return null;
            });

            Instant completedAt = clock.instant();
            context.job = saveJob(context.job, PipelineJobStatus.COMPLETED,
                    PipelineStageName.AI_READY, 100, null, jobStarted, completedAt,
                    context.documentId);
            event(context.job, null, PipelineEventType.JOB_COMPLETED,
                    "Pipeline execution completed.", null);
            counter("marketmind.pipeline.jobs.completed").increment();
            timer("marketmind.pipeline.job.duration", "COMPLETED")
                    .record(Duration.between(jobStarted, completedAt));
        } catch (RuntimeException exception) {
            Instant completedAt = clock.instant();
            context.job = saveJob(context.job, PipelineJobStatus.FAILED,
                    context.job.currentStage(), context.job.progressPercent(),
                    safeMessage(exception), jobStarted, completedAt, context.documentId);
            event(context.job, null, PipelineEventType.JOB_FAILED,
                    "Pipeline execution failed.", safeMessage(exception));
            counter("marketmind.pipeline.jobs.failed").increment();
            timer("marketmind.pipeline.job.duration", "FAILED")
                    .record(Duration.between(jobStarted, completedAt));
            LOGGER.atError()
                    .addKeyValue("pipelineJobId", jobId)
                    .addKeyValue("stage", context.job.currentStage())
                    .log("Pipeline execution failed", exception);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID);
            MDC.remove(CorrelationIdFilter.MDC_REQUEST_ID);
        }
    }

    private <T> T runStage(
            ExecutionContext context,
            PipelineStageName stageName,
            Supplier<T> operation) {
        PipelineStage stage = stage(context.job.id(), stageName);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            Instant startedAt = clock.instant();
            context.job = saveJob(context.job, PipelineJobStatus.RUNNING,
                    stageName, progressBefore(stageName), null,
                    context.job.startedAt(), null, context.documentId);
            stage = saveStage(stage, PipelineStageStatus.RUNNING, attempt,
                    0, null, startedAt, null);
            event(context.job, stage, PipelineEventType.STAGE_STARTED,
                    stageName + " started.", "attempt=" + attempt);
            logStage("started", context.job, stage, 0);
            try {
                T result = operation.get();
                Instant completedAt = clock.instant();
                long duration = duration(startedAt, completedAt);
                stage = saveStage(stage, PipelineStageStatus.COMPLETED, attempt,
                        duration, null, startedAt, completedAt);
                context.job = saveJob(context.job, PipelineJobStatus.RUNNING,
                        stageName, progress(stageName), null,
                        context.job.startedAt(), null, context.documentId);
                event(context.job, stage, PipelineEventType.STAGE_COMPLETED,
                        stageName + " completed.", "durationMs=" + duration);
                logStage("completed", context.job, stage, duration);
                counter("marketmind.pipeline.stages.completed", stageName).increment();
                stageTimer(stageName)
                        .record(Duration.ofMillis(duration));
                return result;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                Instant failedAt = clock.instant();
                long duration = duration(startedAt, failedAt);
                if (attempt < properties.maxAttempts()) {
                    Duration backoff = properties.initialBackoff()
                            .multipliedBy(1L << (attempt - 1));
                    event(context.job, stage, PipelineEventType.STAGE_RETRYING,
                            stageName + " will be retried.",
                            "attempt=" + attempt + ", backoffMs=" + backoff.toMillis()
                                    + ", error=" + safeMessage(exception));
                    counter("marketmind.pipeline.stages.retried", stageName).increment();
                    logStage("retrying", context.job, stage, duration);
                    sleeper.sleep(backoff);
                } else {
                    stage = saveStage(stage, PipelineStageStatus.FAILED, attempt,
                            duration, safeMessage(exception), startedAt, failedAt);
                    event(context.job, stage, PipelineEventType.STAGE_FAILED,
                            stageName + " failed.", safeMessage(exception));
                    counter("marketmind.pipeline.stages.failed", stageName).increment();
                    logStage("failed", context.job, stage, duration);
                }
            }
        }
        throw lastFailure == null
                ? new IllegalStateException("Pipeline stage failed.")
                : lastFailure;
    }

    private void runDerivedStage(
            ExecutionContext context,
            PipelineStageName stageName,
            Runnable validation) {
        runStage(context, stageName, () -> {
            validation.run();
            return null;
        });
    }

    private void skipStage(
            ExecutionContext context,
            PipelineStageName stageName,
            String reason) {
        Instant now = clock.instant();
        PipelineStage stage = saveStage(
                stage(context.job.id(), stageName),
                PipelineStageStatus.SKIPPED,
                0,
                0,
                reason,
                now,
                now);
        event(context.job, stage, PipelineEventType.STAGE_SKIPPED,
                stageName + " skipped.", reason);
    }

    private void initializeStages(PipelineJob job) {
        Instant now = clock.instant();
        for (PipelineStageName stageName : STAGES) {
            repository.saveStage(new PipelineStage(
                    UUID.randomUUID(), job.id(), stageName,
                    PipelineStageStatus.PENDING, 0, properties.maxAttempts(),
                    0, null, null, null, now, now));
        }
    }

    private PipelineStage stage(UUID jobId, PipelineStageName name) {
        return repository.findStages(jobId).stream()
                .filter(stage -> stage.stageName() == name)
                .findFirst()
                .orElseThrow();
    }

    private PipelineStage saveStage(
            PipelineStage stage,
            PipelineStageStatus status,
            int attempt,
            long durationMs,
            String error,
            Instant startedAt,
            Instant completedAt) {
        return repository.saveStage(new PipelineStage(
                stage.id(), stage.pipelineJobId(), stage.stageName(), status,
                attempt, stage.maxAttempts(), durationMs, truncate(error),
                startedAt, completedAt, stage.createdAt(), clock.instant()));
    }

    private PipelineJob saveJob(
            PipelineJob job,
            PipelineJobStatus status,
            PipelineStageName stage,
            int progress,
            String error,
            Instant startedAt,
            Instant completedAt) {
        return saveJob(job, status, stage, progress, error,
                startedAt, completedAt, job.documentId());
    }

    private PipelineJob saveJob(
            PipelineJob job,
            PipelineJobStatus status,
            PipelineStageName stage,
            int progress,
            String error,
            Instant startedAt,
            Instant completedAt,
            UUID documentId) {
        return repository.saveJob(new PipelineJob(
                job.id(), job.discoveredDocumentId(), documentId,
                job.correlationId(), status, stage, progress, truncate(error),
                startedAt, completedAt, job.createdAt(), clock.instant()));
    }

    private void event(
            PipelineJob job,
            PipelineStage stage,
            PipelineEventType type,
            String message,
            String details) {
        repository.saveEvent(new PipelineEvent(
                UUID.randomUUID(), job.id(), stage == null ? null : stage.id(),
                type, message, truncate(details), clock.instant()));
    }

    private void markDiscoveredIngested(DiscoveredDocument document) {
        discoveryRepository.saveDocument(new DiscoveredDocument(
                document.id(), document.sourceType(), document.sourceUrl(),
                document.documentUrl(), document.title(), document.companySymbol(),
                document.documentType(), DiscoveredDocumentStatus.INGESTED,
                document.normalizedUrl(), document.firstDiscoveredAt(),
                document.lastSeenAt(), document.seenCount(), document.createdAt(),
                clock.instant()));
    }

    private DownloadDocumentCommand downloadCommand(DiscoveredDocument document) {
        return new DownloadDocumentCommand(
                document.documentUrl(),
                document.title(),
                documentType(document.documentType()),
                null,
                null,
                null,
                null);
    }

    private DocumentType documentType(DiscoveredDocumentType type) {
        return switch (type) {
            case ANNUAL_REPORT -> DocumentType.ANNUAL_REPORT;
            case QUARTERLY_RESULT -> DocumentType.QUARTERLY_RESULT;
            case INVESTOR_PRESENTATION -> DocumentType.INVESTOR_PRESENTATION;
            case CIRCULAR, ANNOUNCEMENT -> DocumentType.EXCHANGE_FILING;
            case UNKNOWN -> DocumentType.OTHER;
        };
    }

    private Document withStatus(Document document, DocumentStatus status) {
        return new Document(
                document.id(), document.companyId(), document.source(),
                document.documentType(), document.title(), document.sourceUrl(),
                document.publicationDate(), document.reportingPeriod(),
                document.fiscalYear(), document.quarter(), status,
                document.currentVersionId(), document.createdAt(), clock.instant());
    }

    private void validateStart(PipelineStartCommand command) {
        if ((command.discoveredDocumentId() == null) == (command.documentId() == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of discoveredDocumentId or documentId is required.");
        }
    }

    private String correlationId(String supplied) {
        return supplied == null || supplied.isBlank()
                ? UUID.randomUUID().toString()
                : supplied.strip();
    }

    private int progress(PipelineStageName stage) {
        return ((stage.ordinal() + 1) * 100) / STAGES.length;
    }

    private int progressBefore(PipelineStageName stage) {
        return (stage.ordinal() * 100) / STAGES.length;
    }

    private long duration(Instant start, Instant end) {
        return Math.max(0, Duration.between(start, end).toMillis());
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }

    private Counter counter(String name, PipelineStageName stage) {
        return Counter.builder(name).tag("stage", stage.name()).register(meterRegistry);
    }

    private Timer timer(String name, String value) {
        return Timer.builder(name).tag("result", value).register(meterRegistry);
    }

    private Timer stageTimer(PipelineStageName stage) {
        return Timer.builder("marketmind.pipeline.stage.duration")
                .tag("stage", stage.name())
                .register(meterRegistry);
    }

    private void logStage(
            String action,
            PipelineJob job,
            PipelineStage stage,
            long durationMs) {
        LOGGER.atInfo()
                .addKeyValue("pipelineJobId", job.id())
                .addKeyValue("correlationId", job.correlationId())
                .addKeyValue("stage", stage.stageName())
                .addKeyValue("stageStatus", stage.status())
                .addKeyValue("attempt", stage.attemptCount())
                .addKeyValue("durationMs", durationMs)
                .log("Pipeline stage " + action);
    }

    private String safeMessage(Throwable exception) {
        return safe(exception.getMessage(), exception.getClass().getSimpleName());
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value) {
        return value == null || value.length() <= 2000
                ? value
                : value.substring(0, 2000);
    }

    private static final class ExecutionContext {
        private PipelineJob job;
        private UUID documentId;
        private final UUID discoveredDocumentId;
        private DiscoveredDocument discovered;

        private ExecutionContext(
                PipelineJob job,
                UUID documentId,
                UUID discoveredDocumentId) {
            this.job = job;
            this.documentId = documentId;
            this.discoveredDocumentId = discoveredDocumentId;
        }
    }
}
