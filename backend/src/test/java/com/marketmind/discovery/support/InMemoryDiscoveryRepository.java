package com.marketmind.discovery.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.marketmind.discovery.application.DiscoveryDocumentFilter;
import com.marketmind.discovery.application.DiscoveryRepository;
import com.marketmind.discovery.application.PageResult;
import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoverySourceRun;

public class InMemoryDiscoveryRepository implements DiscoveryRepository {

    private final List<DiscoveryJob> jobs = new ArrayList<>();
    private final List<DiscoverySourceRun> sourceRuns = new ArrayList<>();
    private final List<DiscoveredDocument> documents = new ArrayList<>();

    @Override
    public DiscoveryJob saveJob(DiscoveryJob job) {
        jobs.removeIf(existing -> existing.id().equals(job.id()));
        jobs.add(job);
        return job;
    }

    @Override
    public DiscoverySourceRun saveSourceRun(DiscoverySourceRun sourceRun) {
        sourceRuns.removeIf(existing -> existing.id().equals(sourceRun.id()));
        sourceRuns.add(sourceRun);
        return sourceRun;
    }

    @Override
    public DiscoveredDocument saveDocument(DiscoveredDocument document) {
        documents.removeIf(existing -> existing.id().equals(document.id()));
        documents.add(document);
        return document;
    }

    @Override
    public Optional<DiscoveryJob> findJob(UUID id) {
        return jobs.stream().filter(job -> job.id().equals(id)).findFirst();
    }

    @Override
    public PageResult<DiscoveryJob> findJobs(int page, int size) {
        return page(jobs.stream()
                .sorted(Comparator.comparing(DiscoveryJob::createdAt).reversed())
                .toList(), page, size);
    }

    @Override
    public List<DiscoverySourceRun> findSourceRuns(UUID jobId) {
        return sourceRuns.stream()
                .filter(sourceRun -> sourceRun.discoveryJobId().equals(jobId))
                .toList();
    }

    @Override
    public Optional<DiscoveredDocument> findDocument(UUID id) {
        return documents.stream()
                .filter(document -> document.id().equals(id))
                .findFirst();
    }

    @Override
    public Optional<DiscoveredDocument> findByNormalizedUrl(String normalizedUrl) {
        return documents.stream()
                .filter(document -> document.normalizedUrl().equals(normalizedUrl))
                .findFirst();
    }

    @Override
    public PageResult<DiscoveredDocument> findDocuments(
            DiscoveryDocumentFilter filter,
            int page,
            int size) {
        Stream<DiscoveredDocument> stream = documents.stream();
        if (filter.status() != null) {
            stream = stream.filter(document -> document.status() == filter.status());
        }
        if (filter.sourceType() != null) {
            stream = stream.filter(document -> document.sourceType() == filter.sourceType());
        }
        if (filter.documentType() != null) {
            stream = stream.filter(
                    document -> document.documentType() == filter.documentType());
        }
        if (filter.companySymbol() != null && !filter.companySymbol().isBlank()) {
            stream = stream.filter(document -> document.companySymbol() != null
                    && document.companySymbol().equalsIgnoreCase(
                            filter.companySymbol().trim()));
        }
        return page(stream
                .sorted(Comparator.comparing(
                        DiscoveredDocument::lastSeenAt).reversed())
                .toList(), page, size);
    }

    private <T> PageResult<T> page(List<T> values, int page, int size) {
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        int totalPages = values.isEmpty()
                ? 0
                : (int) Math.ceil((double) values.size() / size);
        return new PageResult<>(
                values.subList(from, to),
                page,
                size,
                values.size(),
                totalPages);
    }
}
