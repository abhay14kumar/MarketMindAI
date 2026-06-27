package com.marketmind.discovery.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoveryJobStatus;
import com.marketmind.discovery.domain.DiscoverySourceRun;
import com.marketmind.sourceintelligence.application.SourceConnector;
import com.marketmind.sourceintelligence.application.SourceConnectorFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class DiscoveryService {

    private final DiscoveryRepository repository;
    private final SourceConnectorFactory connectorFactory;
    private final DiscoveryDeduplicationService deduplicationService;
    private final DiscoveryClassificationService classificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DiscoveryService(
            DiscoveryRepository repository,
            SourceConnectorFactory connectorFactory,
            DiscoveryDeduplicationService deduplicationService,
            DiscoveryClassificationService classificationService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.repository = repository;
        this.connectorFactory = connectorFactory;
        this.deduplicationService = deduplicationService;
        this.classificationService = classificationService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public DiscoveryJobDetails run(DiscoveryRunCommand command) {
        validateCommand(command);
        Instant startedAt = clock.instant();
        DiscoveryJob job = repository.saveJob(new DiscoveryJob(
                UUID.randomUUID(),
                command.sourceType(),
                command.sourceUrl(),
                DiscoveryJobStatus.STARTED,
                0,
                0,
                0,
                0,
                0,
                "Discovery started.",
                null,
                null,
                false,
                false,
                0,
                0,
                null,
                null,
                startedAt,
                null,
                startedAt));
        DiscoverySourceRun sourceRun = repository.saveSourceRun(
                new DiscoverySourceRun(
                        UUID.randomUUID(),
                        job.id(),
                        command.sourceType(),
                        command.sourceUrl(),
                        DiscoveryJobStatus.STARTED,
                        0,
                        null,
                        null,
                        0,
                        0,
                        0,
                        0,
                        null,
                        startedAt,
                        null,
                        startedAt));

        try {
            SourceConnector.ConnectorRequest connectorRequest =
                    new SourceConnector.ConnectorRequest(
                            command.sourceType(),
                            command.sourceUrl(),
                            normalizeSymbol(command.companySymbol()),
                            command.maxDocuments());
            SourceConnector connector = connectorFactory.select(connectorRequest);
            SourceConnector.ConnectorResult crawlResult = connector.discover(connectorRequest);
            List<SourceConnector.ConnectorDocument> candidates = crawlResult.documents();
            int newDocuments = 0;
            int existingDocuments = 0;
            int ignoredDocuments = 0;
            List<String> errors = new ArrayList<>();
            for (SourceConnector.ConnectorDocument candidate : candidates) {
                try {
                    switch (persistCandidate(command, candidate)) {
                        case NEW -> newDocuments++;
                        case EXISTING -> existingDocuments++;
                        case IGNORED -> ignoredDocuments++;
                    }
                } catch (RuntimeException exception) {
                    errors.add(safeMessage(exception));
                }
            }

            DiscoveryJobStatus status = errors.isEmpty()
                    ? DiscoveryJobStatus.COMPLETED
                    : DiscoveryJobStatus.PARTIAL;
            Instant completedAt = clock.instant();
            String zeroReason = candidates.isEmpty()
                    ? "No direct PDF links were found in the fetched HTML. The page may be "
                            + "dynamic, protected, or require a source-specific crawler."
                    : null;
            boolean nseGeneric = command.sourceUrl() != null
                    && command.sourceUrl().getHost() != null
                    && command.sourceUrl().getHost().toLowerCase(Locale.ROOT)
                            .contains("nseindia.com")
                    && crawlResult.connectorType()
                            == com.marketmind.sourceintelligence.domain.SourceConnectorType.NSE;
            String message = candidates.isEmpty()
                    ? "Discovery completed but no documents were found."
                    : "Discovery completed and documents were found.";
            if (nseGeneric) {
                message = "NSE pages often require source-specific APIs or browser/session "
                        + "handling. Generic PDF link extraction may return zero results.";
            }
            String recommendation = candidates.isEmpty()
                    ? nseGeneric
                            ? "Try TEST_SOURCE for validation. An NSE-specific crawler is planned."
                            : "Try TEST_SOURCE for validation or use a page that exposes direct PDF links."
                    : "Review discovered documents and start ingestion for the documents you trust.";
            sourceRun = repository.saveSourceRun(new DiscoverySourceRun(
                    sourceRun.id(),
                    sourceRun.discoveryJobId(),
                    sourceRun.sourceType(),
                    sourceRun.sourceUrl(),
                    status,
                    candidates.size(),
                    crawlResult.connectorType().name(),
                    crawlResult.httpStatus(),
                    crawlResult.fetchedBytes(),
                    crawlResult.linksScanned(),
                    crawlResult.documentLinksFound(),
                    crawlResult.skippedLinks(),
                    summarize(errors),
                    sourceRun.startedAt(),
                    completedAt,
                    sourceRun.createdAt()));
            job = repository.saveJob(new DiscoveryJob(
                    job.id(),
                    job.sourceType(),
                    job.sourceUrl(),
                    status,
                    candidates.size(),
                    newDocuments,
                    existingDocuments,
                    ignoredDocuments,
                    0,
                    message,
                    recommendation,
                    crawlResult.connectorType().name(),
                    crawlResult.sourceReachable(),
                    crawlResult.contentFetched(),
                    crawlResult.linksScanned(),
                    crawlResult.documentLinksFound(),
                    zeroReason,
                    summarize(errors),
                    job.startedAt(),
                    completedAt,
                    job.createdAt()));
        } catch (RuntimeException exception) {
            Instant completedAt = clock.instant();
            String error = safeMessage(exception);
            sourceRun = repository.saveSourceRun(new DiscoverySourceRun(
                    sourceRun.id(),
                    sourceRun.discoveryJobId(),
                    sourceRun.sourceType(),
                    sourceRun.sourceUrl(),
                    DiscoveryJobStatus.FAILED,
                    0,
                    null,
                    null,
                    0,
                    0,
                    0,
                    0,
                    error,
                    sourceRun.startedAt(),
                    completedAt,
                    sourceRun.createdAt()));
            job = repository.saveJob(new DiscoveryJob(
                    job.id(),
                    job.sourceType(),
                    job.sourceUrl(),
                    DiscoveryJobStatus.FAILED,
                    0,
                    0,
                    0,
                    0,
                    1,
                    "Discovery failed before the source could be processed.",
                    "Validate the source URL and retry. Use TEST_SOURCE to verify the discovery system.",
                    null,
                    false,
                    false,
                    0,
                    0,
                    null,
                    error,
                    job.startedAt(),
                    completedAt,
                    job.createdAt()));
        }
        return new DiscoveryJobDetails(job, List.of(sourceRun));
    }

    @Transactional(readOnly = true)
    public PageResult<DiscoveryJob> getJobs(int page, int size) {
        validatePage(page, size);
        return repository.findJobs(page, size);
    }

    @Transactional(readOnly = true)
    public DiscoveryJobDetails getJob(UUID id) {
        DiscoveryJob job = repository.findJob(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discovery job not found: " + id));
        return new DiscoveryJobDetails(job, repository.findSourceRuns(id));
    }

    @Transactional(readOnly = true)
    public PageResult<DiscoveredDocument> getDocuments(
            DiscoveryDocumentFilter filter,
            int page,
            int size) {
        validatePage(page, size);
        return repository.findDocuments(filter, page, size);
    }

    @Transactional(readOnly = true)
    public DiscoveredDocument getDocument(UUID id) {
        return repository.findDocument(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discovered document not found: " + id));
    }

    @Transactional
    public DiscoveredDocument ignore(UUID id) {
        return updateStatus(id, DiscoveredDocumentStatus.IGNORED);
    }

    @Transactional
    public DiscoveredDocument markExisting(UUID id) {
        return updateStatus(id, DiscoveredDocumentStatus.EXISTING);
    }

    private CandidateOutcome persistCandidate(
            DiscoveryRunCommand command,
            SourceConnector.ConnectorDocument candidate) {
        String normalizedUrl = deduplicationService.normalize(candidate.documentUrl());
        Instant now = clock.instant();
        var existing = repository.findByNormalizedUrl(normalizedUrl);
        if (existing.isPresent()) {
            DiscoveredDocument current = existing.orElseThrow();
            DiscoveredDocumentStatus status = current.status() == DiscoveredDocumentStatus.IGNORED
                    ? DiscoveredDocumentStatus.IGNORED
                    : DiscoveredDocumentStatus.EXISTING;
            repository.saveDocument(new DiscoveredDocument(
                    current.id(),
                    command.sourceType(),
                    command.sourceUrl(),
                    candidate.documentUrl(),
                    title(candidate),
                    firstNonBlank(
                            normalizeSymbol(command.companySymbol()),
                            current.companySymbol()),
                    classificationService.classify(
                            candidate.title(), candidate.documentUrl()),
                    status,
                    current.normalizedUrl(),
                    current.firstDiscoveredAt(),
                    now,
                    current.seenCount() + 1,
                    current.createdAt(),
                    now));
            return status == DiscoveredDocumentStatus.IGNORED
                    ? CandidateOutcome.IGNORED
                    : CandidateOutcome.EXISTING;
        }

        DiscoveredDocument created = repository.saveDocument(new DiscoveredDocument(
                UUID.randomUUID(),
                command.sourceType(),
                command.sourceUrl(),
                candidate.documentUrl(),
                title(candidate),
                normalizeSymbol(command.companySymbol()),
                classificationService.classify(
                        candidate.title(), candidate.documentUrl()),
                DiscoveredDocumentStatus.NEW,
                normalizedUrl,
                now,
                now,
                1,
                now,
                now));
        eventPublisher.publishEvent(new DiscoveredDocumentCreatedEvent(created.id()));
        return CandidateOutcome.NEW;
    }

    private DiscoveredDocument updateStatus(
            UUID id,
            DiscoveredDocumentStatus status) {
        DiscoveredDocument current = getDocument(id);
        return repository.saveDocument(new DiscoveredDocument(
                current.id(),
                current.sourceType(),
                current.sourceUrl(),
                current.documentUrl(),
                current.title(),
                current.companySymbol(),
                current.documentType(),
                status,
                current.normalizedUrl(),
                current.firstDiscoveredAt(),
                current.lastSeenAt(),
                current.seenCount(),
                current.createdAt(),
                clock.instant()));
    }

    private void validateCommand(DiscoveryRunCommand command) {
        if (command.sourceType() == null) {
            throw new IllegalArgumentException("sourceType is required.");
        }
        if (command.sourceType()
                != com.marketmind.discovery.domain.DiscoverySourceType.TEST_SOURCE
                && command.sourceUrl() == null) {
            throw new IllegalArgumentException(
                    "sourceUrl is required unless sourceType is TEST_SOURCE.");
        }
        if (command.maxDocuments() < 1 || command.maxDocuments() > 100) {
            throw new IllegalArgumentException(
                    "maxDocuments must be between 1 and 100.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private String title(SourceConnector.ConnectorDocument candidate) {
        String value = candidate.title() == null || candidate.title().isBlank()
                ? candidate.documentUrl().toString()
                : candidate.title().strip();
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null || symbol.isBlank()
                ? null
                : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : truncate(message);
    }

    private String summarize(List<String> errors) {
        return errors.isEmpty() ? null : truncate(String.join("; ", errors));
    }

    private String truncate(String value) {
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private enum CandidateOutcome {
        NEW,
        EXISTING,
        IGNORED
    }
}
