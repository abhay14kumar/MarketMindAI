package com.marketmind.documents.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DownloadStatus;
import com.marketmind.documents.domain.DownloadJob;

import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentCatalog documentCatalog;
    private final Clock clock;

    public DocumentService(DocumentCatalog documentCatalog, Clock clock) {
        this.documentCatalog = documentCatalog;
        this.clock = clock;
    }

    public PageResult<Document> getDocuments(int page, int size) {
        return page(documentCatalog.findAllDocuments(), page, size);
    }

    public Document getDocument(UUID id) {
        return documentCatalog.findDocumentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    public PageResult<DownloadJob> getDownloadJobs(int page, int size) {
        return page(documentCatalog.findAllJobs(), page, size);
    }

    public List<DocumentSource> getSources() {
        return documentCatalog.findAllSources();
    }

    public DocumentSource createSource(CreateDocumentSourceCommand command) {
        String normalizedCode = command.code().trim().toUpperCase(Locale.ROOT);
        if (documentCatalog.existsSourceByCode(normalizedCode)) {
            throw new ConflictException("A document source with the same code already exists.");
        }
        Instant now = clock.instant();
        return documentCatalog.saveSource(new DocumentSource(
                UUID.randomUUID(),
                normalizedCode,
                command.name().trim(),
                command.sourceType(),
                command.baseUrl(),
                command.enabled(),
                null,
                now,
                now));
    }

    public DownloadJob queueDownload(DownloadDocumentCommand command) {
        Instant submittedAt = clock.instant();
        DownloadJob job = new DownloadJob(
                UUID.randomUUID(),
                command.documentId(),
                command.sourceId(),
                command.sourceUrl(),
                DownloadStatus.QUEUED,
                0,
                command.maxAttempts(),
                null,
                submittedAt,
                null,
                null,
                submittedAt,
                null,
                null);
        return documentCatalog.saveJob(job);
    }

    public DownloadJob retryDownload(UUID jobId) {
        DownloadJob failedJob = documentCatalog.findJobById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Download job not found: " + jobId));
        if (failedJob.status() != DownloadStatus.FAILED) {
            throw new ConflictException("Only failed download jobs can be retried.");
        }
        Instant submittedAt = clock.instant();
        return documentCatalog.saveJob(new DownloadJob(
                UUID.randomUUID(),
                failedJob.documentId(),
                failedJob.sourceId(),
                failedJob.requestedUrl(),
                DownloadStatus.QUEUED,
                0,
                failedJob.maxAttempts(),
                failedJob.id(),
                submittedAt,
                null,
                null,
                submittedAt,
                null,
                null));
    }

    private <T> PageResult<T> page(List<T> items, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        long requestedOffset = (long) page * size;
        int fromIndex = (int) Math.min(requestedOffset, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PageResult<>(
                items.subList(fromIndex, toIndex),
                page,
                size,
                items.size(),
                totalPages);
    }
}
