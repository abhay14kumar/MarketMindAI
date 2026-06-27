package com.marketmind.documents.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ApiException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.DownloadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDownloadService.class);

    private final DocumentCatalog documentCatalog;
    private final Downloader downloader;
    private final StorageProvider storageProvider;
    private final ChecksumService checksumService;
    private final VersionManager versionManager;
    private final DocumentDownloadPolicy downloadPolicy;
    private final DocumentProcessingTrigger processingTrigger;
    private final Clock clock;

    public DocumentDownloadService(
            DocumentCatalog documentCatalog,
            Downloader downloader,
            StorageProvider storageProvider,
            ChecksumService checksumService,
            VersionManager versionManager,
            DocumentDownloadPolicy downloadPolicy,
            DocumentProcessingTrigger processingTrigger,
            Clock clock) {
        this.documentCatalog = documentCatalog;
        this.downloader = downloader;
        this.storageProvider = storageProvider;
        this.checksumService = checksumService;
        this.versionManager = versionManager;
        this.downloadPolicy = downloadPolicy;
        this.processingTrigger = processingTrigger;
        this.clock = clock;
    }

    public DocumentDownloadResult download(DownloadDocumentCommand command) {
        return download(command, true);
    }

    public DocumentDownloadResult downloadWithoutProcessing(
            DownloadDocumentCommand command) {
        return download(command, false);
    }

    private DocumentDownloadResult download(
            DownloadDocumentCommand command,
            boolean triggerAutomatedProcessing) {
        validateUrl(command.sourceUrl());
        DocumentSource source = resolveSource(command.sourceId());
        Instant startedAt = clock.instant();
        Document existing = documentCatalog.findDocumentBySourceUrl(command.sourceUrl()).orElse(null);
        DownloadJob startedJob = documentCatalog.saveJob(new DownloadJob(
                UUID.randomUUID(),
                existing == null ? null : existing.id(),
                command.sourceId(),
                command.sourceUrl(),
                DownloadStatus.STARTED,
                1,
                1,
                null,
                startedAt,
                startedAt,
                null,
                null,
                null,
                null));

        Downloader.DownloadResult downloaded = null;
        try {
            downloaded = downloader.download(new Downloader.DownloadRequest(
                    command.sourceUrl(),
                    Math.toIntExact(downloadPolicy.maxFileSizeBytes())));
            String checksum = checksum(downloaded.temporaryFile());
            if (versionManager.findByChecksum(checksum).isPresent()) {
                throw DocumentPipelineException.duplicate(checksum);
            }

            UUID documentId = existing == null ? UUID.randomUUID() : existing.id();
            try (InputStream content = Files.newInputStream(downloaded.temporaryFile())) {
                StorageProvider.StoredObject stored = storageProvider.store(
                        new StorageProvider.StoreRequest(
                                downloaded.originalFileName(),
                                downloaded.contentType(),
                                downloaded.sizeBytes(),
                                checksum,
                                downloaded.downloadedAt(),
                                content));
                Document acquired = documentCatalog.saveDocument(toDocument(
                        existing,
                        documentId,
                        source,
                        command,
                        DocumentStatus.ACQUIRED,
                        null,
                        downloaded.downloadedAt()));
                DocumentVersion version = versionManager.createVersion(
                        documentId,
                        checksum,
                        stored.storageReference(),
                        downloaded.contentType(),
                        downloaded.sizeBytes(),
                        downloaded.downloadedAt());
                Document completed = documentCatalog.saveDocument(toDocument(
                        acquired,
                        documentId,
                        source,
                        command,
                        DocumentStatus.COMPLETED,
                        version.id(),
                        downloaded.downloadedAt()));
                DownloadJob completedJob = documentCatalog.saveJob(new DownloadJob(
                        startedJob.id(),
                        completed.id(),
                        command.sourceId(),
                        command.sourceUrl(),
                        DownloadStatus.COMPLETED,
                        1,
                        1,
                        null,
                        startedJob.submittedAt(),
                        startedJob.startedAt(),
                        clock.instant(),
                        null,
                        null,
                        null));
                DocumentDownloadResult result =
                        new DocumentDownloadResult(completedJob, completed, version);
                if (triggerAutomatedProcessing) {
                    triggerProcessing(completed.id());
                }
                return result;
            }
        } catch (ApiException exception) {
            failJob(startedJob, exception.getErrorCode().name(), exception.getMessage());
            throw exception;
        } catch (IOException exception) {
            failJob(startedJob, "STORAGE_FAILURE", "Unable to read the downloaded document.");
            throw DocumentPipelineException.storageFailure(
                    "Unable to read the downloaded document.", exception);
        } catch (RuntimeException exception) {
            failJob(startedJob, "DOWNLOAD_FAILED", "The document acquisition failed.");
            throw DocumentPipelineException.downloadFailed(
                    "The document acquisition failed.", exception);
        } finally {
            deleteTemporaryFile(downloaded);
        }
    }

    private void triggerProcessing(UUID documentId) {
        try {
            processingTrigger.documentDownloaded(documentId);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unable to submit document {} for automated processing",
                    documentId,
                    exception);
        }
    }

    public List<DocumentVersion> getVersions(UUID documentId) {
        if (documentCatalog.findDocumentById(documentId).isEmpty()) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        return versionManager.findVersions(documentId);
    }

    private Document toDocument(
            Document existing,
            UUID documentId,
            DocumentSource source,
            DownloadDocumentCommand command,
            DocumentStatus status,
            UUID currentVersionId,
            Instant acquiredAt) {
        Instant createdAt = existing == null ? clock.instant() : existing.createdAt();
        return new Document(
                documentId,
                command.companyId(),
                source,
                command.documentType(),
                command.title().trim(),
                command.sourceUrl(),
                LocalDate.ofInstant(acquiredAt, ZoneOffset.UTC),
                reportingPeriod(command.fiscalYear(), command.quarter()),
                command.fiscalYear(),
                normalizedQuarter(command.quarter()),
                status,
                currentVersionId,
                createdAt,
                clock.instant());
    }

    private DocumentSource resolveSource(UUID sourceId) {
        if (sourceId == null) {
            return null;
        }
        return documentCatalog.findSourceById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document source not found: " + sourceId));
    }

    private String checksum(Path temporaryFile) throws IOException {
        try (InputStream content = Files.newInputStream(temporaryFile)) {
            return checksumService.sha256(content);
        }
    }

    private void failJob(DownloadJob startedJob, String errorCode, String message) {
        documentCatalog.saveJob(new DownloadJob(
                startedJob.id(),
                startedJob.documentId(),
                startedJob.sourceId(),
                startedJob.requestedUrl(),
                DownloadStatus.FAILED,
                startedJob.attemptCount(),
                startedJob.maxAttempts(),
                startedJob.retryOfJobId(),
                startedJob.submittedAt(),
                startedJob.startedAt(),
                clock.instant(),
                null,
                errorCode,
                safeMessage(message)));
    }

    private void deleteTemporaryFile(Downloader.DownloadResult downloaded) {
        if (downloaded == null) {
            return;
        }
        try {
            Files.deleteIfExists(downloaded.temporaryFile());
        } catch (IOException ignored) {
            // Temporary-file cleanup failure must not hide the acquisition result.
        }
    }

    private void validateUrl(URI sourceUrl) {
        String scheme = sourceUrl.getScheme() == null
                ? ""
                : sourceUrl.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("http") && !scheme.equals("https"))
                || sourceUrl.getHost() == null
                || sourceUrl.getHost().isBlank()
                || sourceUrl.getUserInfo() != null) {
            throw DocumentPipelineException.invalidUrl(
                    "The source URL must be a valid HTTP or HTTPS URL without user information.");
        }
    }

    private String reportingPeriod(Integer fiscalYear, String quarter) {
        if (fiscalYear == null) {
            return null;
        }
        String normalizedQuarter = normalizedQuarter(quarter);
        return normalizedQuarter == null
                ? "FY" + fiscalYear
                : "FY" + fiscalYear + " " + normalizedQuarter;
    }

    private String normalizedQuarter(String quarter) {
        return quarter == null || quarter.isBlank()
                ? null
                : quarter.trim().toUpperCase(Locale.ROOT);
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Document acquisition failed.";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
