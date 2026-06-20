package com.marketmind.documents.api;

import java.net.URI;

import com.marketmind.documents.application.DownloadDocumentCommand;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DownloadJob;

import org.springframework.stereotype.Component;

@Component
public class DocumentApiMapper {

    public DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.id(),
                document.companyId(),
                document.source().code(),
                document.source().name(),
                document.documentType(),
                document.title(),
                document.sourceUrl().toString(),
                document.publicationDate(),
                document.reportingPeriod(),
                document.status(),
                document.currentVersionId(),
                document.createdAt(),
                document.updatedAt());
    }

    public DownloadJobResponse toResponse(DownloadJob job) {
        return new DownloadJobResponse(
                job.id(),
                job.documentId(),
                job.sourceId(),
                job.requestedUrl().toString(),
                job.status(),
                job.attemptCount(),
                job.maxAttempts(),
                job.submittedAt(),
                job.startedAt(),
                job.completedAt(),
                job.nextAttemptAt(),
                job.errorCode(),
                job.errorMessage());
    }

    public DownloadDocumentCommand toCommand(DownloadDocumentRequest request) {
        return new DownloadDocumentCommand(
                request.documentId(),
                request.sourceId(),
                URI.create(request.sourceUrl()),
                request.maxAttempts());
    }
}
