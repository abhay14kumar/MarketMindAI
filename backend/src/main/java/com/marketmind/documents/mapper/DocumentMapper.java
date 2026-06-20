package com.marketmind.documents.mapper;

import java.net.URI;

import com.marketmind.documents.application.CreateDocumentSourceCommand;
import com.marketmind.documents.application.DownloadDocumentCommand;
import com.marketmind.documents.application.PageResult;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.dto.CreateDocumentSourceRequest;
import com.marketmind.documents.dto.DocumentResponse;
import com.marketmind.documents.dto.DocumentSourceResponse;
import com.marketmind.documents.dto.DownloadDocumentRequest;
import com.marketmind.documents.dto.DownloadJobResponse;
import com.marketmind.documents.dto.PageResponse;

import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

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
                job.retryOfJobId(),
                job.submittedAt(),
                job.startedAt(),
                job.completedAt(),
                job.nextAttemptAt(),
                job.errorCode(),
                job.errorMessage());
    }

    public DocumentSourceResponse toResponse(DocumentSource source) {
        return new DocumentSourceResponse(
                source.id(),
                source.code(),
                source.name(),
                source.sourceType(),
                source.baseUrl().toString(),
                source.enabled(),
                source.lastCheckedAt(),
                source.createdAt(),
                source.updatedAt());
    }

    public PageResponse<DocumentResponse> toDocumentPage(PageResult<Document> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public PageResponse<DownloadJobResponse> toJobPage(PageResult<DownloadJob> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public DownloadDocumentCommand toCommand(DownloadDocumentRequest request) {
        return new DownloadDocumentCommand(
                request.documentId(),
                request.sourceId(),
                URI.create(request.sourceUrl()),
                request.maxAttempts());
    }

    public CreateDocumentSourceCommand toCommand(CreateDocumentSourceRequest request) {
        return new CreateDocumentSourceCommand(
                request.code(),
                request.name(),
                request.sourceType(),
                URI.create(request.baseUrl()),
                request.enabled());
    }
}
