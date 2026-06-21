package com.marketmind.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
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
import com.marketmind.documents.application.PdfTextExtractionService;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.documents.infrastructure.InMemoryDocumentCatalog;
import com.marketmind.pipeline.domain.DocumentPipelineRun;
import com.marketmind.pipeline.domain.DocumentPipelineStep;
import com.marketmind.pipeline.domain.PipelineStatus;
import com.marketmind.pipeline.domain.PipelineStepName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentPipelineServiceTest {

    private static final UUID DOCUMENT_ID =
            UUID.fromString("91000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID =
            UUID.fromString("91000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-06-21T12:00:00Z");

    private InMemoryPipelineRepository repository;
    private InMemoryDocumentCatalog documentCatalog;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPipelineRepository();
        documentCatalog = new InMemoryDocumentCatalog();
        documentCatalog.saveDocument(document());
    }

    @Test
    void shouldCompletePipelineAndMarkDocumentAiReady() {
        StubEmbeddingService embeddingService = new StubEmbeddingService(
                completedEmbeddingJob());
        DocumentPipelineService service = service(
                extraction(ExtractionStatus.COMPLETED, null),
                embeddingService);

        DocumentPipelineRun result = service.processDownloaded(DOCUMENT_ID);

        assertThat(result.status()).isEqualTo(PipelineStatus.COMPLETED);
        assertThat(result.currentStep()).isEqualTo(PipelineStepName.AI_READY);
        assertThat(documentCatalog.findDocumentById(DOCUMENT_ID).orElseThrow().status())
                .isEqualTo(DocumentStatus.AI_READY);
        assertThat(repository.steps)
                .anyMatch(step -> step.stepName() == PipelineStepName.AI_READY
                        && step.status() == PipelineStatus.COMPLETED);
        assertThat(embeddingService.calls).isEqualTo(1);
    }

    @Test
    void shouldStopAndPersistSkippedStepsWhenExtractionFails() {
        StubEmbeddingService embeddingService = new StubEmbeddingService(
                completedEmbeddingJob());
        DocumentPipelineService service = service(
                extraction(ExtractionStatus.FAILED, "Unreadable PDF."),
                embeddingService);

        DocumentPipelineRun result = service.processDownloaded(DOCUMENT_ID);

        assertThat(result.status()).isEqualTo(PipelineStatus.FAILED);
        assertThat(result.currentStep()).isEqualTo(PipelineStepName.TEXT_EXTRACTION);
        assertThat(result.errorMessage()).isEqualTo("Unreadable PDF.");
        assertThat(embeddingService.calls).isZero();
        assertThat(repository.steps).anyMatch(step ->
                step.stepName() == PipelineStepName.TEXT_EXTRACTION
                        && step.status() == PipelineStatus.FAILED);
        assertThat(repository.steps).anyMatch(step ->
                step.stepName() == PipelineStepName.AI_READY
                        && step.status() == PipelineStatus.SKIPPED);
    }

    private DocumentPipelineService service(
            DocumentTextExtraction extraction,
            StubEmbeddingService embeddingService) {
        return new DocumentPipelineService(
                repository,
                documentCatalog,
                new StubExtractionService(extraction),
                embeddingService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private DocumentTextExtraction extraction(
            ExtractionStatus status,
            String errorMessage) {
        return new DocumentTextExtraction(
                UUID.randomUUID(),
                DOCUMENT_ID,
                VERSION_ID,
                status,
                status == ExtractionStatus.COMPLETED ? "Extracted text" : null,
                1,
                14L,
                errorMessage,
                NOW,
                NOW);
    }

    private DocumentEmbeddingJob completedEmbeddingJob() {
        return new DocumentEmbeddingJob(
                UUID.randomUUID(),
                DOCUMENT_ID,
                VERSION_ID,
                EmbeddingJobStatus.COMPLETED,
                3,
                3,
                0,
                null,
                NOW,
                NOW,
                NOW);
    }

    private Document document() {
        return new Document(
                DOCUMENT_ID,
                null,
                null,
                DocumentType.ANNUAL_REPORT,
                "Annual Report",
                URI.create("https://example.invalid/report.pdf"),
                null,
                null,
                2026,
                null,
                DocumentStatus.COMPLETED,
                VERSION_ID,
                NOW,
                NOW);
    }

    private static final class StubExtractionService
            extends PdfTextExtractionService {

        private final DocumentTextExtraction result;

        private StubExtractionService(DocumentTextExtraction result) {
            super(null, null, null, null, null);
            this.result = result;
        }

        @Override
        public DocumentTextExtraction extract(UUID documentId) {
            return result;
        }
    }

    private static final class StubEmbeddingService
            extends DocumentEmbeddingService {

        private final DocumentEmbeddingJob result;
        private int calls;

        private StubEmbeddingService(DocumentEmbeddingJob result) {
            super(null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public DocumentEmbeddingJob embed(UUID documentId) {
            calls++;
            return result;
        }
    }

    private static final class InMemoryPipelineRepository
            implements DocumentPipelineRepository {

        private final List<DocumentPipelineRun> runs = new ArrayList<>();
        private final List<DocumentPipelineStep> steps = new ArrayList<>();

        @Override
        public DocumentPipelineRun saveRun(DocumentPipelineRun run) {
            runs.removeIf(existing -> existing.id().equals(run.id()));
            runs.add(run);
            return run;
        }

        @Override
        public DocumentPipelineStep saveStep(DocumentPipelineStep step) {
            steps.removeIf(existing -> existing.id().equals(step.id()));
            steps.add(step);
            return step;
        }

        @Override
        public PageResult<PipelineRunSummary> findRuns(int page, int size) {
            List<PipelineRunSummary> summaries = runs.stream()
                    .sorted(Comparator.comparing(
                            DocumentPipelineRun::createdAt).reversed())
                    .map(run -> new PipelineRunSummary(run, "Annual Report"))
                    .toList();
            return new PageResult<>(
                    summaries, page, size, summaries.size(),
                    summaries.isEmpty() ? 0 : 1);
        }

        @Override
        public Optional<PipelineRunSummary> findRun(UUID runId) {
            return runs.stream()
                    .filter(run -> run.id().equals(runId))
                    .findFirst()
                    .map(run -> new PipelineRunSummary(run, "Annual Report"));
        }

        @Override
        public Optional<PipelineRunSummary> findLatestRun(UUID documentId) {
            return runs.stream()
                    .filter(run -> run.documentId().equals(documentId))
                    .max(Comparator.comparing(DocumentPipelineRun::createdAt))
                    .map(run -> new PipelineRunSummary(run, "Annual Report"));
        }

        @Override
        public List<DocumentPipelineStep> findSteps(UUID runId) {
            return steps.stream()
                    .filter(step -> step.pipelineRunId().equals(runId))
                    .toList();
        }

        @Override
        public int nextRetryCount(UUID documentId, PipelineStepName stepName) {
            return steps.stream()
                    .filter(step -> step.documentId().equals(documentId))
                    .filter(step -> step.stepName() == stepName)
                    .mapToInt(DocumentPipelineStep::retryCount)
                    .max()
                    .orElse(-1) + 1;
        }
    }
}
