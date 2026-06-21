package com.marketmind.documents.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.documents.domain.SourceType;

import org.junit.jupiter.api.Test;

class PdfTextExtractionServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Test
    void shouldPersistCompletedExtraction() {
        TestCatalog catalog = new TestCatalog("application/pdf");
        TestExtractionRepository repository = new TestExtractionRepository();
        Parser parser = request -> new Parser.ParseResult(
                "Extracted annual report text", java.util.Map.of("pageCount", "3"));
        PdfTextExtractionService service = new PdfTextExtractionService(
                catalog, repository, storage(), parser,
                Clock.fixed(NOW, ZoneOffset.UTC));

        DocumentTextExtraction extraction = service.extract(DOCUMENT_ID);

        assertThat(extraction.extractionStatus()).isEqualTo(ExtractionStatus.COMPLETED);
        assertThat(extraction.pageCount()).isEqualTo(3);
        assertThat(extraction.characterCount()).isEqualTo(28);
        assertThat(repository.saved)
                .extracting(DocumentTextExtraction::extractionStatus)
                .containsExactly(ExtractionStatus.STARTED, ExtractionStatus.COMPLETED);
    }

    @Test
    void shouldMarkScannedPdfUnsupportedWhenNoTextIsExtracted() {
        TestExtractionRepository repository = new TestExtractionRepository();
        PdfTextExtractionService service = new PdfTextExtractionService(
                new TestCatalog("application/pdf"), repository, storage(),
                request -> new Parser.ParseResult("", java.util.Map.of("pageCount", "2")),
                Clock.fixed(NOW, ZoneOffset.UTC));

        DocumentTextExtraction extraction = service.extract(DOCUMENT_ID);

        assertThat(extraction.extractionStatus()).isEqualTo(ExtractionStatus.UNSUPPORTED);
        assertThat(extraction.errorMessage()).contains("OCR is not enabled");
    }

    @Test
    void shouldMarkNonPdfVersionUnsupported() {
        PdfTextExtractionService service = new PdfTextExtractionService(
                new TestCatalog("text/html"), new TestExtractionRepository(), storage(),
                request -> {
                    throw new AssertionError("Parser must not be invoked for non-PDF content.");
                },
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.extract(DOCUMENT_ID).extractionStatus())
                .isEqualTo(ExtractionStatus.UNSUPPORTED);
    }

    private StorageProvider storage() {
        return new StorageProvider() {
            @Override
            public StoredObject store(StoreRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream load(String storageReference) {
                return new ByteArrayInputStream(new byte[] {1, 2, 3});
            }
        };
    }

    private static final class TestExtractionRepository
            implements DocumentTextExtractionRepository {

        private final List<DocumentTextExtraction> saved = new ArrayList<>();

        @Override
        public DocumentTextExtraction save(DocumentTextExtraction extraction) {
            saved.add(extraction);
            return extraction;
        }

        @Override
        public Optional<DocumentTextExtraction> findLatestByDocumentId(UUID documentId) {
            return saved.stream()
                    .filter(value -> value.documentId().equals(documentId))
                    .reduce((first, second) -> second);
        }
    }

    private static final class TestCatalog implements DocumentCatalog {

        private final Document document;
        private final DocumentVersion version;

        private TestCatalog(String mimeType) {
            DocumentSource source = new DocumentSource(
                    UUID.randomUUID(), "TEST", "Test", SourceType.MANUAL,
                    URI.create("https://example.invalid"), true, NOW, NOW, NOW);
            document = new Document(
                    DOCUMENT_ID, null, source, DocumentType.ANNUAL_REPORT,
                    "Annual Report", URI.create("https://example.invalid/report.pdf"),
                    null, null, null, null, DocumentStatus.COMPLETED,
                    VERSION_ID, NOW, NOW);
            version = new DocumentVersion(
                    VERSION_ID, DOCUMENT_ID, 1,
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "stored/report.pdf", mimeType, 3, NOW, NOW);
        }

        @Override public List<Document> findAllDocuments() { return List.of(document); }
        @Override public Optional<Document> findDocumentById(UUID id) { return id.equals(DOCUMENT_ID) ? Optional.of(document) : Optional.empty(); }
        @Override public Optional<Document> findDocumentBySourceUrl(URI sourceUrl) { return Optional.empty(); }
        @Override public Document saveDocument(Document value) { return value; }
        @Override public List<DownloadJob> findAllJobs() { return List.of(); }
        @Override public Optional<DownloadJob> findJobById(UUID id) { return Optional.empty(); }
        @Override public DownloadJob saveJob(DownloadJob job) { return job; }
        @Override public Optional<DocumentSource> findSourceById(UUID id) { return Optional.empty(); }
        @Override public List<DocumentSource> findAllSources() { return List.of(); }
        @Override public boolean existsSourceByCode(String code) { return false; }
        @Override public DocumentSource saveSource(DocumentSource source) { return source; }
        @Override public Optional<DocumentVersion> findVersionByChecksum(String checksum) { return Optional.empty(); }
        @Override public List<DocumentVersion> findVersionsByDocumentId(UUID documentId) { return List.of(version); }
        @Override public DocumentVersion saveVersion(DocumentVersion value) { return value; }
    }
}
