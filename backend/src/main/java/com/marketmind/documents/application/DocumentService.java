package com.marketmind.documents.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.DownloadJobStatus;

import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentCatalog documentCatalog;
    private final Clock clock;

    public DocumentService(DocumentCatalog documentCatalog, Clock clock) {
        this.documentCatalog = documentCatalog;
        this.clock = clock;
    }

    public List<Document> getDocuments() {
        return documentCatalog.findAllDocuments();
    }

    public Document getDocument(UUID id) {
        return documentCatalog.findDocumentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    public List<DownloadJob> getDownloadJobs() {
        return documentCatalog.findAllJobs();
    }

    public DownloadJob queueDownload(DownloadDocumentCommand command) {
        Instant submittedAt = clock.instant();
        DownloadJob job = new DownloadJob(
                UUID.randomUUID(),
                command.documentId(),
                command.sourceId(),
                command.sourceUrl(),
                DownloadJobStatus.QUEUED,
                0,
                command.maxAttempts(),
                submittedAt,
                null,
                null,
                submittedAt,
                null,
                null);
        return documentCatalog.saveJob(job);
    }
}
