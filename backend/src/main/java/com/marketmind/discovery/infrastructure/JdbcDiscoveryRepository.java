package com.marketmind.discovery.infrastructure;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.discovery.application.DiscoveryDocumentFilter;
import com.marketmind.discovery.application.DiscoveryRepository;
import com.marketmind.discovery.application.PageResult;
import com.marketmind.discovery.domain.DiscoveredDocument;
import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoveryJob;
import com.marketmind.discovery.domain.DiscoveryJobStatus;
import com.marketmind.discovery.domain.DiscoverySourceRun;
import com.marketmind.discovery.domain.DiscoverySourceType;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDiscoveryRepository implements DiscoveryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDiscoveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DiscoveryJob saveJob(DiscoveryJob job) {
        jdbcTemplate.update("""
                INSERT INTO discovery_job (
                    id, source_type, source_url, status, total_discovered,
                    new_documents, existing_documents, ignored_documents, failed_sources,
                    message, recommendation, crawler_type_used, source_reachable,
                    html_fetched, links_scanned, pdf_links_found, reason_when_zero_results,
                    error_message, started_at, completed_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    total_discovered = EXCLUDED.total_discovered,
                    new_documents = EXCLUDED.new_documents,
                    existing_documents = EXCLUDED.existing_documents,
                    ignored_documents = EXCLUDED.ignored_documents,
                    failed_sources = EXCLUDED.failed_sources,
                    message = EXCLUDED.message,
                    recommendation = EXCLUDED.recommendation,
                    crawler_type_used = EXCLUDED.crawler_type_used,
                    source_reachable = EXCLUDED.source_reachable,
                    html_fetched = EXCLUDED.html_fetched,
                    links_scanned = EXCLUDED.links_scanned,
                    pdf_links_found = EXCLUDED.pdf_links_found,
                    reason_when_zero_results = EXCLUDED.reason_when_zero_results,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at
                """,
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
                timestamp(job.startedAt()),
                timestamp(job.completedAt()),
                timestamp(job.createdAt()));
        return job;
    }

    @Override
    public DiscoverySourceRun saveSourceRun(DiscoverySourceRun sourceRun) {
        jdbcTemplate.update("""
                INSERT INTO discovery_source_run (
                    id, discovery_job_id, source_type, source_url, status,
                    discovered_count, crawler_type, http_status, fetched_html_bytes,
                    total_links_found, pdf_links_found, skipped_links_count,
                    error_message, started_at, completed_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    discovered_count = EXCLUDED.discovered_count,
                    crawler_type = EXCLUDED.crawler_type,
                    http_status = EXCLUDED.http_status,
                    fetched_html_bytes = EXCLUDED.fetched_html_bytes,
                    total_links_found = EXCLUDED.total_links_found,
                    pdf_links_found = EXCLUDED.pdf_links_found,
                    skipped_links_count = EXCLUDED.skipped_links_count,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at
                """,
                sourceRun.id(),
                sourceRun.discoveryJobId(),
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
                timestamp(sourceRun.startedAt()),
                timestamp(sourceRun.completedAt()),
                timestamp(sourceRun.createdAt()));
        return sourceRun;
    }

    @Override
    public DiscoveredDocument saveDocument(DiscoveredDocument document) {
        jdbcTemplate.update("""
                INSERT INTO discovered_document (
                    id, source_type, source_url, document_url, title,
                    company_symbol, document_type, status, normalized_url,
                    first_discovered_at, last_seen_at, seen_count, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    source_type = EXCLUDED.source_type,
                    source_url = EXCLUDED.source_url,
                    document_url = EXCLUDED.document_url,
                    title = EXCLUDED.title,
                    company_symbol = EXCLUDED.company_symbol,
                    document_type = EXCLUDED.document_type,
                    status = EXCLUDED.status,
                    last_seen_at = EXCLUDED.last_seen_at,
                    seen_count = EXCLUDED.seen_count,
                    updated_at = EXCLUDED.updated_at
                """,
                document.id(),
                document.sourceType().name(),
                string(document.sourceUrl()),
                document.documentUrl().toString(),
                document.title(),
                document.companySymbol(),
                document.documentType().name(),
                document.status().name(),
                document.normalizedUrl(),
                timestamp(document.firstDiscoveredAt()),
                timestamp(document.lastSeenAt()),
                document.seenCount(),
                timestamp(document.createdAt()),
                timestamp(document.updatedAt()));
        return document;
    }

    @Override
    public Optional<DiscoveryJob> findJob(UUID id) {
        return jdbcTemplate.query("""
                SELECT * FROM discovery_job WHERE id = ?
                """, this::mapJob, id).stream().findFirst();
    }

    @Override
    public PageResult<DiscoveryJob> findJobs(int page, int size) {
        long total = count("SELECT COUNT(*) FROM discovery_job", List.of());
        List<DiscoveryJob> content = jdbcTemplate.query("""
                SELECT * FROM discovery_job
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, this::mapJob, size, (long) page * size);
        return page(content, page, size, total);
    }

    @Override
    public List<DiscoverySourceRun> findSourceRuns(UUID jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM discovery_source_run
                WHERE discovery_job_id = ?
                ORDER BY created_at
                """, this::mapSourceRun, jobId);
    }

    @Override
    public Optional<DiscoveredDocument> findDocument(UUID id) {
        return jdbcTemplate.query("""
                SELECT * FROM discovered_document WHERE id = ?
                """, this::mapDocument, id).stream().findFirst();
    }

    @Override
    public Optional<DiscoveredDocument> findByNormalizedUrl(String normalizedUrl) {
        return jdbcTemplate.query("""
                SELECT * FROM discovered_document WHERE normalized_url = ?
                """, this::mapDocument, normalizedUrl).stream().findFirst();
    }

    @Override
    public PageResult<DiscoveredDocument> findDocuments(
            DiscoveryDocumentFilter filter,
            int page,
            int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> arguments = new ArrayList<>();
        addFilter(where, arguments, "status", filter.status());
        addFilter(where, arguments, "source_type", filter.sourceType());
        addFilter(where, arguments, "document_type", filter.documentType());
        if (filter.companySymbol() != null && !filter.companySymbol().isBlank()) {
            where.append(" AND UPPER(company_symbol) = UPPER(?)");
            arguments.add(filter.companySymbol().trim());
        }

        long total = count(
                "SELECT COUNT(*) FROM discovered_document" + where,
                arguments);
        List<Object> queryArguments = new ArrayList<>(arguments);
        queryArguments.add(size);
        queryArguments.add((long) page * size);
        List<DiscoveredDocument> content = jdbcTemplate.query(
                "SELECT * FROM discovered_document"
                        + where
                        + " ORDER BY last_seen_at DESC LIMIT ? OFFSET ?",
                this::mapDocument,
                queryArguments.toArray());
        return page(content, page, size, total);
    }

    private void addFilter(
            StringBuilder where,
            List<Object> arguments,
            String column,
            Enum<?> value) {
        if (value != null) {
            where.append(" AND ").append(column).append(" = ?");
            arguments.add(value.name());
        }
    }

    private long count(String sql, List<Object> arguments) {
        Long value = jdbcTemplate.queryForObject(
                sql, Long.class, arguments.toArray());
        return value == null ? 0 : value;
    }

    private <T> PageResult<T> page(
            List<T> content,
            int page,
            int size,
            long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private DiscoveryJob mapJob(ResultSet resultSet, int row) throws SQLException {
        return new DiscoveryJob(
                resultSet.getObject("id", UUID.class),
                DiscoverySourceType.valueOf(resultSet.getString("source_type")),
                uri(resultSet.getString("source_url")),
                DiscoveryJobStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("total_discovered"),
                resultSet.getInt("new_documents"),
                resultSet.getInt("existing_documents"),
                resultSet.getInt("ignored_documents"),
                resultSet.getInt("failed_sources"),
                resultSet.getString("message"),
                resultSet.getString("recommendation"),
                resultSet.getString("crawler_type_used"),
                resultSet.getBoolean("source_reachable"),
                resultSet.getBoolean("html_fetched"),
                resultSet.getInt("links_scanned"),
                resultSet.getInt("pdf_links_found"),
                resultSet.getString("reason_when_zero_results"),
                resultSet.getString("error_message"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"));
    }

    private DiscoverySourceRun mapSourceRun(
            ResultSet resultSet,
            int row) throws SQLException {
        return new DiscoverySourceRun(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("discovery_job_id", UUID.class),
                DiscoverySourceType.valueOf(resultSet.getString("source_type")),
                uri(resultSet.getString("source_url")),
                DiscoveryJobStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("discovered_count"),
                resultSet.getString("crawler_type"),
                (Integer) resultSet.getObject("http_status"),
                resultSet.getLong("fetched_html_bytes"),
                resultSet.getInt("total_links_found"),
                resultSet.getInt("pdf_links_found"),
                resultSet.getInt("skipped_links_count"),
                resultSet.getString("error_message"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"));
    }

    private DiscoveredDocument mapDocument(
            ResultSet resultSet,
            int row) throws SQLException {
        return new DiscoveredDocument(
                resultSet.getObject("id", UUID.class),
                DiscoverySourceType.valueOf(resultSet.getString("source_type")),
                uri(resultSet.getString("source_url")),
                URI.create(resultSet.getString("document_url")),
                resultSet.getString("title"),
                resultSet.getString("company_symbol"),
                DiscoveredDocumentType.valueOf(resultSet.getString("document_type")),
                DiscoveredDocumentStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("normalized_url"),
                instant(resultSet, "first_discovered_at"),
                instant(resultSet, "last_seen_at"),
                resultSet.getInt("seen_count"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private URI uri(String value) {
        return value == null ? null : URI.create(value);
    }

    private String string(URI value) {
        return value == null ? null : value.toString();
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
