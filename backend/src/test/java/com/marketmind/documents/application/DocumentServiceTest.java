package com.marketmind.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.DownloadJobStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID DOCUMENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID =
            UUID.fromString("51000000-0000-0000-0000-000000000001");

    private InMemoryDocumentCatalog catalog;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        catalog = new InMemoryDocumentCatalog();
        service = new DocumentService(catalog, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnDocuments() {
        assertThat(service.getDocuments())
                .extracting(Document::title)
                .containsExactly("Annual Report");
    }

    @Test
    void shouldReturnDocumentById() {
        assertThat(service.getDocument(DOCUMENT_ID).id()).isEqualTo(DOCUMENT_ID);
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
        DownloadDocumentCommand command = new DownloadDocumentCommand(
                DOCUMENT_ID,
                SOURCE_ID,
                URI.create("https://example.invalid/annual-report.pdf"),
                3);

        DownloadJob job = service.queueDownload(command);

        assertThat(job.id()).isNotNull();
        assertThat(job.status()).isEqualTo(DownloadJobStatus.QUEUED);
        assertThat(job.attemptCount()).isZero();
        assertThat(job.maxAttempts()).isEqualTo(3);
        assertThat(job.submittedAt()).isEqualTo(NOW);
        assertThat(catalog.jobs).containsExactly(job);
    }

    private static final class InMemoryDocumentCatalog implements DocumentCatalog {

        private final Document document;
        private final List<DownloadJob> jobs = new ArrayList<>();

        private InMemoryDocumentCatalog() {
            DocumentSource source = new DocumentSource(
                    SOURCE_ID,
                    "NSE",
                    "National Stock Exchange of India",
                    "EXCHANGE",
                    URI.create("https://example.invalid"),
                    true,
                    NOW,
                    NOW,
                    NOW);
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
        public DownloadJob saveJob(DownloadJob job) {
            jobs.add(job);
            return job;
        }
    }
}
