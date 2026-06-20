package com.marketmind.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.DownloadStatus;
import com.marketmind.documents.domain.SourceType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID DOCUMENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID =
            UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID FAILED_JOB_ID =
            UUID.fromString("55000000-0000-0000-0000-000000000002");

    private InMemoryDocumentCatalog catalog;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        catalog = new InMemoryDocumentCatalog();
        service = new DocumentService(catalog, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnPaginatedDocuments() {
        PageResult<Document> result = service.getDocuments(0, 20);

        assertThat(result.content()).extracting(Document::title).containsExactly("Annual Report");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturnNotFoundForUnknownDocument() {
        UUID missingId = UUID.fromString("53000000-0000-0000-0000-000000000099");

        assertThatThrownBy(() -> service.getDocument(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void shouldQueueDownloadWithoutCallingExternalServices() {
        DownloadJob job = service.queueDownload(new DownloadDocumentCommand(
                DOCUMENT_ID,
                SOURCE_ID,
                URI.create("https://example.invalid/annual-report.pdf"),
                3));

        assertThat(job.status()).isEqualTo(DownloadStatus.QUEUED);
        assertThat(job.attemptCount()).isZero();
        assertThat(job.submittedAt()).isEqualTo(NOW);
        assertThat(catalog.jobs).contains(job);
    }

    @Test
    void shouldRetryFailedDownloadAsNewJob() {
        DownloadJob retried = service.retryDownload(FAILED_JOB_ID);

        assertThat(retried.id()).isNotEqualTo(FAILED_JOB_ID);
        assertThat(retried.retryOfJobId()).isEqualTo(FAILED_JOB_ID);
        assertThat(retried.status()).isEqualTo(DownloadStatus.QUEUED);
    }

    @Test
    void shouldRejectRetryForNonFailedJob() {
        assertThatThrownBy(() -> service.retryDownload(
                        UUID.fromString("55000000-0000-0000-0000-000000000001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("failed");
    }

    @Test
    void shouldCreateNormalizedDocumentSource() {
        DocumentSource source = service.createSource(new CreateDocumentSourceCommand(
                " sebi ",
                "Securities and Exchange Board of India",
                SourceType.REGULATOR,
                URI.create("https://www.sebi.gov.in"),
                true));

        assertThat(source.id()).isNotNull();
        assertThat(source.code()).isEqualTo("SEBI");
        assertThat(source.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectDuplicateDocumentSourceCode() {
        assertThatThrownBy(() -> service.createSource(new CreateDocumentSourceCommand(
                        "nse",
                        "Duplicate NSE",
                        SourceType.EXCHANGE,
                        URI.create("https://example.invalid"),
                        true)))
                .isInstanceOf(ConflictException.class);
    }

    private static final class InMemoryDocumentCatalog implements DocumentCatalog {

        private final Document document;
        private final List<DocumentSource> sources = new ArrayList<>();
        private final List<DownloadJob> jobs = new ArrayList<>();

        private InMemoryDocumentCatalog() {
            DocumentSource source = new DocumentSource(
                    SOURCE_ID,
                    "NSE",
                    "National Stock Exchange of India",
                    SourceType.EXCHANGE,
                    URI.create("https://example.invalid"),
                    true,
                    NOW,
                    NOW,
                    NOW);
            sources.add(source);
            document = new Document(
                    DOCUMENT_ID,
                    null,
                    source,
                    DocumentType.ANNUAL_REPORT,
                    "Annual Report",
                    URI.create("https://example.invalid/annual-report.pdf"),
                    null,
                    "FY2025-26",
                    DocumentStatus.COMPLETED,
                    null,
                    NOW,
                    NOW);
            jobs.add(job(
                    UUID.fromString("55000000-0000-0000-0000-000000000001"),
                    DownloadStatus.COMPLETED));
            jobs.add(job(FAILED_JOB_ID, DownloadStatus.FAILED));
        }

        @Override
        public List<Document> findAllDocuments() {
            return List.of(document);
        }

        @Override
        public Optional<Document> findDocumentById(UUID id) {
            return document.id().equals(id) ? Optional.of(document) : Optional.empty();
        }

        @Override
        public List<DownloadJob> findAllJobs() {
            return List.copyOf(jobs);
        }

        @Override
        public Optional<DownloadJob> findJobById(UUID id) {
            return jobs.stream().filter(job -> job.id().equals(id)).findFirst();
        }

        @Override
        public DownloadJob saveJob(DownloadJob job) {
            jobs.add(job);
            return job;
        }

        @Override
        public List<DocumentSource> findAllSources() {
            return List.copyOf(sources);
        }

        @Override
        public boolean existsSourceByCode(String code) {
            return sources.stream()
                    .anyMatch(source -> source.code().toUpperCase(Locale.ROOT)
                            .equals(code.toUpperCase(Locale.ROOT)));
        }

        @Override
        public DocumentSource saveSource(DocumentSource source) {
            sources.add(source);
            return source;
        }

        private DownloadJob job(UUID id, DownloadStatus status) {
            return new DownloadJob(
                    id,
                    DOCUMENT_ID,
                    SOURCE_ID,
                    URI.create("https://example.invalid/annual-report.pdf"),
                    status,
                    status == DownloadStatus.FAILED ? 3 : 1,
                    3,
                    null,
                    NOW.minusSeconds(60),
                    NOW.minusSeconds(30),
                    NOW,
                    null,
                    status == DownloadStatus.FAILED ? "MOCK_FAILURE" : null,
                    status == DownloadStatus.FAILED ? "Synthetic failure." : null);
        }
    }
}
