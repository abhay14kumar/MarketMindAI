package com.marketmind.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.DownloadStatus;
import com.marketmind.documents.infrastructure.DefaultVersionManager;
import com.marketmind.documents.infrastructure.DocumentDownloadProperties;
import com.marketmind.documents.infrastructure.DocumentStorageProperties;
import com.marketmind.documents.infrastructure.InMemoryDocumentCatalog;
import com.marketmind.documents.infrastructure.LocalFileStorageProvider;
import com.marketmind.documents.infrastructure.Sha256ChecksumService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentDownloadServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");

    @TempDir
    Path storageRoot;

    private InMemoryDocumentCatalog catalog;
    private DocumentDownloadService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        catalog = new InMemoryDocumentCatalog();
        service = new DocumentDownloadService(
                catalog,
                new StubDownloader("annual report"),
                new LocalFileStorageProvider(new DocumentStorageProperties(storageRoot)),
                new Sha256ChecksumService(),
                new DefaultVersionManager(catalog, clock),
                new DocumentDownloadProperties(30, 1),
                clock);
    }

    @Test
    void shouldDownloadStoreAndVersionDocument() {
        DocumentDownloadResult result = service.download(command(
                "https://example.invalid/reliance-annual-report.pdf"));

        assertThat(result.job().status()).isEqualTo(DownloadStatus.COMPLETED);
        assertThat(result.document().currentVersionId()).isEqualTo(result.version().id());
        assertThat(result.document().fiscalYear()).isEqualTo(2026);
        assertThat(result.version().versionNumber()).isEqualTo(1);
        assertThat(storageRoot.resolve(result.version().storageReference())).exists();
    }

    @Test
    void shouldRejectDuplicateChecksumAndRecordFailedJob() {
        service.download(command("https://example.invalid/first.pdf"));

        assertThatThrownBy(() -> service.download(command("https://example.invalid/second.pdf")))
                .isInstanceOf(DocumentPipelineException.class)
                .hasMessageContaining("checksum");

        assertThat(catalog.findAllJobs())
                .anyMatch(job -> job.requestedUrl().toString().endsWith("second.pdf")
                        && job.status() == DownloadStatus.FAILED
                        && "DUPLICATE_DOCUMENT".equals(job.errorCode()));
    }

    @Test
    void shouldRecordFailedJobWhenDownloadFails() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        DocumentDownloadService failingService = new DocumentDownloadService(
                catalog,
                request -> {
                    throw DocumentPipelineException.downloadFailed("Synthetic failure.");
                },
                new LocalFileStorageProvider(new DocumentStorageProperties(storageRoot)),
                new Sha256ChecksumService(),
                new DefaultVersionManager(catalog, clock),
                new DocumentDownloadProperties(30, 1),
                clock);

        assertThatThrownBy(() -> failingService.download(command(
                        "https://example.invalid/failure.pdf")))
                .isInstanceOf(DocumentPipelineException.class);

        assertThat(catalog.findAllJobs())
                .anyMatch(job -> job.status() == DownloadStatus.FAILED
                        && "DOWNLOAD_FAILED".equals(job.errorCode()));
    }

    private DownloadDocumentCommand command(String sourceUrl) {
        return new DownloadDocumentCommand(
                URI.create(sourceUrl),
                "Reliance Industries Annual Report",
                DocumentType.ANNUAL_REPORT,
                null,
                null,
                2026,
                null);
    }

    private static final class StubDownloader implements Downloader {

        private final byte[] content;

        private StubDownloader(String content) {
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public DownloadResult download(DownloadRequest request) {
            try {
                Path temporaryFile = Files.createTempFile("document-test-", ".pdf");
                Files.write(temporaryFile, content);
                return new DownloadResult(
                        temporaryFile,
                        "application/pdf",
                        NOW,
                        "annual-report.pdf",
                        content.length);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
