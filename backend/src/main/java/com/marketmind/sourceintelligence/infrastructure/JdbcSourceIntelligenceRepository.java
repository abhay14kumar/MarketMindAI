package com.marketmind.sourceintelligence.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.sourceintelligence.application.SourceCoverageRow;
import com.marketmind.sourceintelligence.application.SourceIntelligenceMetrics;
import com.marketmind.sourceintelligence.application.SourceIntelligenceRepository;
import com.marketmind.sourceintelligence.domain.SourceActivity;
import com.marketmind.sourceintelligence.domain.SourceActivityType;
import com.marketmind.sourceintelligence.domain.SourceConnectorType;
import com.marketmind.sourceintelligence.domain.SourceFormat;
import com.marketmind.sourceintelligence.domain.SourceIntelligenceProfile;
import com.marketmind.sourceintelligence.domain.SourceTrustTier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSourceIntelligenceRepository implements SourceIntelligenceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSourceIntelligenceRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SourceIntelligenceProfile> findProfile(UUID sourceId) {
        return jdbcTemplate.query(
                "SELECT * FROM source_intelligence_profile WHERE source_id = ?",
                this::mapProfile,
                sourceId).stream().findFirst();
    }

    @Override
    public SourceIntelligenceProfile saveProfile(SourceIntelligenceProfile profile) {
        jdbcTemplate.update("""
                INSERT INTO source_intelligence_profile (
                    source_id, connector_type, trust_tier, trust_score, freshness_score,
                    supported_formats, supported_document_types, last_crawl_at, next_crawl_at,
                    scheduler_state, total_crawls, successful_crawls, failed_crawls,
                    documents_discovered, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_id) DO UPDATE SET
                    connector_type = EXCLUDED.connector_type,
                    trust_tier = EXCLUDED.trust_tier,
                    trust_score = EXCLUDED.trust_score,
                    freshness_score = EXCLUDED.freshness_score,
                    supported_formats = EXCLUDED.supported_formats,
                    supported_document_types = EXCLUDED.supported_document_types,
                    last_crawl_at = EXCLUDED.last_crawl_at,
                    next_crawl_at = EXCLUDED.next_crawl_at,
                    scheduler_state = EXCLUDED.scheduler_state,
                    total_crawls = EXCLUDED.total_crawls,
                    successful_crawls = EXCLUDED.successful_crawls,
                    failed_crawls = EXCLUDED.failed_crawls,
                    documents_discovered = EXCLUDED.documents_discovered,
                    updated_at = EXCLUDED.updated_at
                """,
                profile.sourceId(),
                profile.connectorType().name(),
                profile.trustTier().name(),
                profile.trustScore(),
                profile.freshnessScore(),
                json(profile.supportedFormats()),
                json(profile.supportedDocumentTypes()),
                timestamp(profile.lastCrawlAt()),
                timestamp(profile.nextCrawlAt()),
                profile.schedulerState(),
                profile.totalCrawls(),
                profile.successfulCrawls(),
                profile.failedCrawls(),
                profile.documentsDiscovered(),
                timestamp(profile.createdAt()),
                timestamp(profile.updatedAt()));
        return profile;
    }

    @Override
    public SourceActivity saveActivity(SourceActivity activity) {
        jdbcTemplate.update("""
                INSERT INTO source_activity (
                    id, source_id, activity_type, severity, title, message,
                    related_entity_type, related_entity_id, occurred_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                activity.id(), activity.sourceId(), activity.activityType().name(),
                activity.severity(), activity.title(), activity.message(),
                activity.relatedEntityType(), activity.relatedEntityId(),
                timestamp(activity.occurredAt()), timestamp(activity.createdAt()));
        return activity;
    }

    @Override
    public List<SourceActivity> findActivity(int limit) {
        return jdbcTemplate.query("""
                SELECT id, source_id, activity_type, severity, title, message,
                       related_entity_type, related_entity_id, occurred_at, created_at
                FROM source_activity
                UNION ALL
                SELECT id, NULL::uuid, 'DISCOVERY', CASE WHEN status = 'FAILED' THEN 'ERROR'
                    WHEN total_discovered = 0 THEN 'WARNING' ELSE 'SUCCESS' END,
                    'Discovery ' || LOWER(status), COALESCE(message, error_message, 'Discovery event'),
                    'DISCOVERY_JOB', id, COALESCE(completed_at, started_at), created_at
                FROM discovery_job
                UNION ALL
                SELECT id, NULL::uuid, 'PIPELINE', CASE WHEN event_type = 'STAGE_FAILED'
                    OR event_type = 'JOB_FAILED' THEN 'ERROR' ELSE 'INFO' END,
                    REPLACE(INITCAP(REPLACE(event_type, '_', ' ')), 'Job ', 'Pipeline '),
                    message, 'PIPELINE_JOB', pipeline_job_id, created_at, created_at
                FROM pipeline_event
                ORDER BY occurred_at DESC
                LIMIT ?
                """, this::mapActivity, limit);
    }

    @Override
    public List<SourceCoverageRow> coverage() {
        return jdbcTemplate.query("""
                SELECT COALESCE(company_symbol, 'UNASSIGNED') AS company_symbol,
                       document_type,
                       COUNT(*) AS discovered_count,
                       COUNT(*) FILTER (WHERE status = 'NEW') AS new_count,
                       COUNT(*) FILTER (WHERE status IN ('INGESTED', 'EXISTING')) AS ingested_count,
                       COUNT(*) FILTER (
                           WHERE EXISTS (
                               SELECT 1 FROM pipeline_job pj
                               WHERE pj.discovered_document_id = dd.id
                                 AND pj.status = 'COMPLETED'
                           )
                       ) AS ai_ready_count
                FROM discovered_document dd
                GROUP BY COALESCE(company_symbol, 'UNASSIGNED'), document_type
                ORDER BY company_symbol, document_type
                """, (resultSet, row) -> {
                    long discovered = resultSet.getLong("discovered_count");
                    long ready = resultSet.getLong("ai_ready_count");
                    String status = ready == discovered && discovered > 0
                            ? "COMPLETE" : ready > 0 ? "PARTIAL" : "MISSING";
                    return new SourceCoverageRow(
                            resultSet.getString("company_symbol"),
                            resultSet.getString("document_type"),
                            discovered,
                            resultSet.getLong("new_count"),
                            resultSet.getLong("ingested_count"),
                            ready,
                            status);
                });
    }

    @Override
    public SourceIntelligenceMetrics metrics() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM source_registry) AS total_sources,
                    (SELECT COUNT(*) FROM source_intelligence_profile WHERE trust_tier = 'OFFICIAL') AS official_sources,
                    (SELECT COUNT(DISTINCT source_id) FROM source_health WHERE available = TRUE) AS healthy_sources,
                    (SELECT COUNT(DISTINCT source_id) FROM source_health WHERE status = 'DEGRADED') AS degraded_sources,
                    (SELECT COUNT(*) FROM source_intelligence_profile) AS enabled_connectors,
                    (SELECT COUNT(*) FROM discovery_job) AS discovery_jobs,
                    (SELECT COUNT(*) FROM pipeline_job) AS pipeline_jobs,
                    (SELECT COUNT(*) FROM discovered_document) AS documents_discovered,
                    COALESCE((SELECT AVG(trust_score) FROM source_intelligence_profile), 0) AS average_trust_score,
                    COALESCE((SELECT AVG(reliability_score) * 100 FROM source_registry), 0) AS average_reliability,
                    COALESCE((
                        SELECT 100.0 * COUNT(*) FILTER (WHERE coverage_count > 0)
                            / NULLIF(COUNT(*), 0)
                        FROM (
                            SELECT company_symbol, document_type, COUNT(*) AS coverage_count
                            FROM discovered_document
                            GROUP BY company_symbol, document_type
                        ) coverage
                    ), 0) AS coverage_percent
                """, (resultSet, row) -> new SourceIntelligenceMetrics(
                resultSet.getLong("total_sources"),
                resultSet.getLong("official_sources"),
                resultSet.getLong("healthy_sources"),
                resultSet.getLong("degraded_sources"),
                resultSet.getLong("enabled_connectors"),
                resultSet.getLong("discovery_jobs"),
                resultSet.getLong("pipeline_jobs"),
                resultSet.getLong("documents_discovered"),
                resultSet.getDouble("average_trust_score"),
                resultSet.getDouble("average_reliability"),
                resultSet.getDouble("coverage_percent")));
    }

    private SourceIntelligenceProfile mapProfile(ResultSet resultSet, int row) throws SQLException {
        return new SourceIntelligenceProfile(
                resultSet.getObject("source_id", UUID.class),
                SourceConnectorType.valueOf(resultSet.getString("connector_type")),
                SourceTrustTier.valueOf(resultSet.getString("trust_tier")),
                resultSet.getInt("trust_score"),
                resultSet.getInt("freshness_score"),
                enumSet(resultSet.getString("supported_formats"), SourceFormat.class),
                enumSet(resultSet.getString("supported_document_types"), DiscoveredDocumentType.class),
                instant(resultSet, "last_crawl_at"),
                instant(resultSet, "next_crawl_at"),
                resultSet.getString("scheduler_state"),
                resultSet.getLong("total_crawls"),
                resultSet.getLong("successful_crawls"),
                resultSet.getLong("failed_crawls"),
                resultSet.getLong("documents_discovered"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private SourceActivity mapActivity(ResultSet resultSet, int row) throws SQLException {
        return new SourceActivity(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("source_id", UUID.class),
                SourceActivityType.valueOf(resultSet.getString("activity_type")),
                resultSet.getString("severity"),
                resultSet.getString("title"),
                resultSet.getString("message"),
                resultSet.getString("related_entity_type"),
                resultSet.getObject("related_entity_id", UUID.class),
                instant(resultSet, "occurred_at"),
                instant(resultSet, "created_at"));
    }

    private <E extends Enum<E>> Set<E> enumSet(String json, Class<E> type) {
        try {
            String[] values = objectMapper.readValue(json, String[].class);
            return Arrays.stream(values).map(value -> Enum.valueOf(type, value))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse source intelligence metadata.", exception);
        }
    }

    private String json(Set<? extends Enum<?>> values) {
        try {
            return objectMapper.writeValueAsString(
                    values.stream().map(Enum::name).sorted().toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize source intelligence metadata.", exception);
        }
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
