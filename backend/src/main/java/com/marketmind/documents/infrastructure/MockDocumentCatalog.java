package com.marketmind.documents.infrastructure;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.marketmind.documents.application.DocumentCatalog;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentSource;
import com.marketmind.documents.domain.DocumentStatus;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.DownloadJob;
import com.marketmind.documents.domain.DownloadStatus;
import com.marketmind.documents.domain.SourceType;

import org.springframework.stereotype.Component;

@Component
public class MockDocumentCatalog implements DocumentCatalog {

    private static final Instant MOCK_TIME = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID NSE_SOURCE_ID =
            UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY_ID =
            UUID.fromString("52000000-0000-0000-0000-000000000001");
    private static final UUID DOCUMENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID FAILED_JOB_ID =
            UUID.fromString("55000000-0000-0000-0000-000000000002");

    private final Map<UUID, Document> documents;
    private final Map<UUID, DocumentSource> sources = new ConcurrentHashMap<>();
    private final Map<UUID, DownloadJob> jobs = new ConcurrentHashMap<>();

    public MockDocumentCatalog() {
        DocumentSource source = new DocumentSource(
                NSE_SOURCE_ID,
                "NSE",
                "National Stock Exchange of India",
                SourceType.EXCHANGE,
                URI.create("https://www.nseindia.com"),
                true,
                MOCK_TIME,
                MOCK_TIME,
                MOCK_TIME);
        Document document = new Document(
                DOCUMENT_ID,
                COMPANY_ID,
                source,
                DocumentType.ANNUAL_REPORT,
                "MarketMind Industries Annual Report 2025-26",
                URI.create("https://example.invalid/marketmind/annual-report-2026.pdf"),
                LocalDate.of(2026, 5, 28),
                "FY2025-26",
                DocumentStatus.COMPLETED,
                UUID.fromString("54000000-0000-0000-0000-000000000001"),
                MOCK_TIME,
                MOCK_TIME);
        documents = Map.of(document.id(), document);

        DownloadJob completedJob = new DownloadJob(
                UUID.fromString("55000000-0000-0000-0000-000000000001"),
                DOCUMENT_ID,
                NSE_SOURCE_ID,
                document.sourceUrl(),
                DownloadStatus.COMPLETED,
                1,
                3,
                null,
                MOCK_TIME.minusSeconds(300),
                MOCK_TIME.minusSeconds(240),
                MOCK_TIME.minusSeconds(180),
                null,
                null,
                null);
        DownloadJob failedJob = new DownloadJob(
                FAILED_JOB_ID,
                DOCUMENT_ID,
                NSE_SOURCE_ID,
                document.sourceUrl(),
                DownloadStatus.FAILED,
                3,
                3,
                null,
                MOCK_TIME.minusSeconds(120),
                MOCK_TIME.minusSeconds(90),
                MOCK_TIME.minusSeconds(30),
                null,
                "MOCK_UPSTREAM_FAILURE",
                "Synthetic provider failure.");
        sources.put(source.id(), source);
        jobs.put(completedJob.id(), completedJob);
        jobs.put(failedJob.id(), failedJob);
    }

    @Override
    public List<Document> findAllDocuments() {
        return documents.values().stream()
                .sorted(Comparator.comparing(Document::publicationDate).reversed())
                .toList();
    }

    @Override
    public Optional<Document> findDocumentById(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public List<DownloadJob> findAllJobs() {
        List<DownloadJob> result = new ArrayList<>(jobs.values());
        result.sort(Comparator.comparing(DownloadJob::submittedAt).reversed());
        return List.copyOf(result);
    }

    @Override
    public Optional<DownloadJob> findJobById(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public DownloadJob saveJob(DownloadJob job) {
        jobs.put(job.id(), job);
        return job;
    }

    @Override
    public List<DocumentSource> findAllSources() {
        return sources.values().stream()
                .sorted(Comparator.comparing(DocumentSource::code))
                .toList();
    }

    @Override
    public boolean existsSourceByCode(String code) {
        return sources.values().stream()
                .anyMatch(source -> source.code().equalsIgnoreCase(code));
    }

    @Override
    public DocumentSource saveSource(DocumentSource source) {
        sources.put(source.id(), source);
        return source;
    }
}
