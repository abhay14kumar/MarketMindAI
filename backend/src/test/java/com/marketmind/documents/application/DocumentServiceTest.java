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
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.SourceType;

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

        private final List<Document> documents = new ArrayList<>();
        private final List<DocumentSource> sources = new ArrayList<>();

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
            documents.add(new Document(
                    DOCUMENT_ID,
                    null,
                    source,
                    DocumentType.ANNUAL_REPORT,
                    "Annual Report",
                    URI.create("https://example.invalid/annual-report.pdf"),
                    null,
                    "FY2026",
                    2026,
                    null,
                    DocumentStatus.COMPLETED,
                    null,
                    NOW,
                    NOW));
        }

        @Override
        public List<Document> findAllDocuments() {
            return List.copyOf(documents);
        }

        @Override
        public Optional<Document> findDocumentById(UUID id) {
            return documents.stream().filter(document -> document.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Document> findDocumentBySourceUrl(URI sourceUrl) {
            return documents.stream()
                    .filter(document -> document.sourceUrl().equals(sourceUrl))
                    .findFirst();
        }

        @Override
        public Document saveDocument(Document document) {
            documents.removeIf(existing -> existing.id().equals(document.id()));
            documents.add(document);
            return document;
        }

        @Override
        public List<DownloadJob> findAllJobs() {
            return List.of();
        }

        @Override
        public Optional<DownloadJob> findJobById(UUID id) {
            return Optional.empty();
        }

        @Override
        public DownloadJob saveJob(DownloadJob job) {
            return job;
        }

        @Override
        public Optional<DocumentSource> findSourceById(UUID id) {
            return sources.stream().filter(source -> source.id().equals(id)).findFirst();
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

        @Override
        public Optional<DocumentVersion> findVersionByChecksum(String checksumSha256) {
            return Optional.empty();
        }

        @Override
        public List<DocumentVersion> findVersionsByDocumentId(UUID documentId) {
            return List.of();
        }

        @Override
        public DocumentVersion saveVersion(DocumentVersion version) {
            return version;
        }
    }
}
