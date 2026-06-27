package com.marketmind.discovery.mapper;

import com.marketmind.discovery.application.DiscoveryJobDetails;
import com.marketmind.discovery.application.DiscoveryRunCommand;
import com.marketmind.discovery.application.PageResult;
import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoverySourceRun;
import com.marketmind.discovery.dto.DiscoveredDocumentResponse;
import com.marketmind.discovery.dto.DiscoveryJobDetailResponse;
import com.marketmind.discovery.dto.DiscoveryJobResponse;
import com.marketmind.discovery.dto.DiscoveryRunRequest;
import com.marketmind.discovery.dto.DiscoverySourceRunResponse;
import com.marketmind.discovery.dto.PageResponse;

import org.springframework.stereotype.Component;

@Component
public class DiscoveryMapper {

    public DiscoveryRunCommand toCommand(DiscoveryRunRequest request) {
        return new DiscoveryRunCommand(
                request.sourceType(),
                request.sourceUrl(),
                request.companySymbol(),
                request.maxDocuments() == null ? 20 : request.maxDocuments());
    }

    public DiscoveryJobResponse toResponse(DiscoveryJob job) {
        return new DiscoveryJobResponse(
                job.id(),
                job.sourceType().name(),
                string(job.sourceUrl()),
                job.status().name(),
                job.totalDiscovered(),
                job.newDocuments(),
                job.existingDocuments(),
                job.ignoredDocuments(),
                job.failedSources(),
                job.message(),
                job.recommendation(),
                job.crawlerTypeUsed(),
                job.sourceReachable(),
                job.htmlFetched(),
                job.linksScanned(),
                job.pdfLinksFound(),
                job.reasonWhenZeroResults(),
                job.errorMessage(),
                job.startedAt(),
                job.completedAt());
    }

    public DiscoveryJobDetailResponse toResponse(DiscoveryJobDetails details) {
        return new DiscoveryJobDetailResponse(
                toResponse(details.job()),
                details.sourceRuns().stream().map(this::toResponse).toList());
    }

    public DiscoveredDocumentResponse toResponse(DiscoveredDocument document) {
        return new DiscoveredDocumentResponse(
                document.id(),
                document.sourceType().name(),
                string(document.sourceUrl()),
                document.documentUrl().toString(),
                document.title(),
                document.companySymbol(),
                document.documentType().name(),
                document.status().name(),
                document.normalizedUrl(),
                document.firstDiscoveredAt(),
                document.lastSeenAt(),
                document.seenCount(),
                document.createdAt(),
                document.updatedAt());
    }

    public PageResponse<DiscoveryJobResponse> toJobPage(PageResult<DiscoveryJob> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public PageResponse<DiscoveredDocumentResponse> toDocumentPage(
            PageResult<DiscoveredDocument> page) {
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private DiscoverySourceRunResponse toResponse(DiscoverySourceRun sourceRun) {
        return new DiscoverySourceRunResponse(
                sourceRun.id(),
                sourceRun.sourceType().name(),
                string(sourceRun.sourceUrl()),
                sourceRun.status().name(),
                sourceRun.discoveredCount(),
                sourceRun.crawlerType(),
                sourceRun.httpStatus(),
                sourceRun.fetchedHtmlBytes(),
                sourceRun.totalLinksFound(),
                sourceRun.pdfLinksFound(),
                sourceRun.skippedLinksCount(),
                sourceRun.errorMessage(),
                sourceRun.startedAt(),
                sourceRun.completedAt());
    }

    private String string(java.net.URI uri) {
        return uri == null ? null : uri.toString();
    }
}
