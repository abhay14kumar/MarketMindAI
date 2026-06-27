package com.marketmind.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.ai.application.DocumentEmbeddingService;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;
import com.marketmind.documents.application.DocumentTextExtractionRepository;
import com.marketmind.documents.application.PdfTextExtractionService;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.documents.infrastructure.InMemoryDocumentCatalog;
import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineJobStatus;
import com.marketmind.pipeline.domain.PipelineStage;
import com.marketmind.pipeline.domain.PipelineStageName;
import com.marketmind.pipeline.domain.PipelineStageStatus;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PipelineOrchestratorTest {

    private static final UUID DOCUMENT_ID =
            UUID.fromString("92000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID =
            UUID.fromString("92000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-06-22T12:00:00Z");

    private InMemoryJobRepository repository;
    private InMemoryDocumentCatalog documents;
    private RecordingSleeper sleeper;

    @BeforeEach
    void setUp() {
        repository = new InMemoryJobRepository();
        documents = new InMemoryDocumentCatalog();
        documents.saveDocument(document());
        sleeper = new RecordingSleeper();
    }

    @Test
    void shouldOrchestrateExistingDocumentToAiReady() {
        PipelineOrchestrator orchestrator = orchestrator(
                new StubExtractionService(0),
                new StubEmbeddingService());

        PipelineJob queued = orchestrator.start(
                new PipelineStartCommand(null, DOCUMENT_ID, "pipeline-test"));
        PipelineJobDetails result = orchestrator.getJob(queued.id());

        assertThat(result.job().status()).isEqualTo(PipelineJobStatus.COMPLETED);
        assertThat(result.job().progressPercent()).isEqualTo(100);
        assertThat(documents.findDocumentById(DOCUMENT_ID).orElseThrow().status())
                .isEqualTo(DocumentStatus.AI_READY);
        assertThat(result.stages())
                .filteredOn(stage -> stage.status() == PipelineStageStatus.COMPLETED)
                .extracting(PipelineStage::stageName)
                .contains(
                        PipelineStageName.TEXT_EXTRACTION,
                        PipelineStageName.CHUNKING,
                        PipelineStageName.EMBEDDING,
                        PipelineStageName.QDRANT_INDEXING,
                        PipelineStageName.AI_SUMMARY,
                        PipelineStageName.AI_READY);
    }

    @Test
    void shouldRetryTransientStageFailureWithExponentialBackoff() {
        StubExtractionService extraction = new StubExtractionService(2);
        PipelineOrchestrator orchestrator = orchestrator(
                extraction,
                new StubEmbeddingService());

        PipelineJob queued = orchestrator.start(
                new PipelineStartCommand(null, DOCUMENT_ID, "retry-test"));
        PipelineJobDetails result = orchestrator.getJob(queued.id());
        PipelineStage extractionStage = result.stages().stream()
                .filter(stage -> stage.stageName() == PipelineStageName.TEXT_EXTRACTION)
                .findFirst()
                .orElseThrow();

        assertThat(result.job().status()).isEqualTo(PipelineJobStatus.COMPLETED);
        assertThat(extraction.calls).isEqualTo(3);
        assertThat(extractionStage.attemptCount()).isEqualTo(3);
        assertThat(sleeper.delays)
                .containsExactly(Duration.ofMillis(10), Duration.ofMillis(20));
        assertThat(result.events())
                .filteredOn(event -> event.eventType().name().equals("STAGE_RETRYING"))
                .hasSize(2);
    }

    private PipelineOrchestrator orchestrator(
            StubExtractionService extraction,
            StubEmbeddingService embedding) {
        DocumentTextExtraction completed = extractionResult();
        DocumentTextExtractionRepository extractionRepository =
                new DocumentTextExtractionRepository() {
                    @Override
                    public DocumentTextExtraction save(DocumentTextExtraction value) {
                        return value;
                    }

                    @Override
                    public Optional<DocumentTextExtraction> findLatestByDocumentId(
                            UUID documentId) {
                        return Optional.of(completed);
                    }
                };
        return new PipelineOrchestrator(
                repository,
                null,
                documents,
                null,
                extraction,
                extractionRepository,
                embedding,
                (question, context) -> "Grounded summary.",
                new PipelineOrchestrationProperties(
                        true, 3, Duration.ofMillis(10), 12000),
                sleeper,
                Runnable::run,
                new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Document document() {
        return new Document(
                DOCUMENT_ID, null, null, DocumentType.ANNUAL_REPORT,
                "Annual Report", URI.create("https://example.com/report.pdf"),
                null, null, 2026, null, DocumentStatus.COMPLETED,
                VERSION_ID, NOW, NOW);
    }

    private DocumentTextExtraction extractionResult() {
        return new DocumentTextExtraction(
                UUID.randomUUID(), DOCUMENT_ID, VERSION_ID,
                ExtractionStatus.COMPLETED, "Extracted financial report text.",
                1, 32L, null, NOW, NOW);
    }

    private final class StubExtractionService extends PdfTextExtractionService {

        private final int failuresBeforeSuccess;
        private int calls;

        private StubExtractionService(int failuresBeforeSuccess) {
            super(null, null, null, null, null);
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public DocumentTextExtraction extract(UUID documentId) {
            calls++;
            if (calls <= failuresBeforeSuccess) {
                throw new IllegalStateException("Transient extraction failure.");
            }
            return extractionResult();
        }
    }

    private static final class StubEmbeddingService
            extends DocumentEmbeddingService {

        private StubEmbeddingService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public DocumentEmbeddingJob embed(UUID documentId) {
            return new DocumentEmbeddingJob(
                    UUID.randomUUID(), DOCUMENT_ID, VERSION_ID,
                    EmbeddingJobStatus.COMPLETED, 3, 3, 0,
                    null, NOW, NOW, NOW);
        }
    }

    private static final class RecordingSleeper implements PipelineSleeper {

        private final List<Duration> delays = new ArrayList<>();

        @Override
        public void sleep(Duration duration) {
            delays.add(duration);
        }
    }

    private static final class InMemoryJobRepository
            implements PipelineJobRepository {

        private final List<PipelineJob> jobs = new ArrayList<>();
        private final List<PipelineStage> stages = new ArrayList<>();
        private final List<PipelineEvent> events = new ArrayList<>();

        @Override
        public PipelineJob saveJob(PipelineJob job) {
            jobs.removeIf(existing -> existing.id().equals(job.id()));
            jobs.add(job);
            return job;
        }

        @Override
        public PipelineStage saveStage(PipelineStage stage) {
            stages.removeIf(existing -> existing.id().equals(stage.id()));
            stages.add(stage);
            return stage;
        }

        @Override
        public PipelineEvent saveEvent(PipelineEvent event) {
            events.add(event);
            return event;
        }

        @Override
        public Optional<PipelineJob> findJob(UUID jobId) {
            return jobs.stream().filter(job -> job.id().equals(jobId)).findFirst();
        }

        @Override
        public PageResult<PipelineJob> findJobs(int page, int size) {
            List<PipelineJob> sorted = jobs.stream()
                    .sorted(Comparator.comparing(PipelineJob::createdAt).reversed())
                    .toList();
            return new PageResult<>(sorted, page, size, sorted.size(),
                    sorted.isEmpty() ? 0 : 1);
        }

        @Override
        public List<PipelineStage> findStages(UUID jobId) {
            return stages.stream()
                    .filter(stage -> stage.pipelineJobId().equals(jobId))
                    .sorted(Comparator.comparing(PipelineStage::createdAt))
                    .toList();
        }

        @Override
        public List<PipelineEvent> findEvents(UUID jobId) {
            return events.stream()
                    .filter(event -> event.pipelineJobId().equals(jobId))
                    .toList();
        }

        @Override
        public PipelineMetrics metrics() {
            long completed = jobs.stream()
                    .filter(job -> job.status() == PipelineJobStatus.COMPLETED)
                    .count();
            long failed = jobs.stream()
                    .filter(job -> job.status() == PipelineJobStatus.FAILED)
                    .count();
            return new PipelineMetrics(
                    jobs.size(), 0, completed, failed,
                    jobs.isEmpty() ? 0 : (double) completed / jobs.size(), 0);
        }
    }
}
