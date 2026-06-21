package com.marketmind.sources.infrastructure;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.sources.application.SourceRegistryRepository;
import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityStatus;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceCapability;
import com.marketmind.sources.domain.SourceHealth;
import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;
import com.marketmind.sources.domain.SourceValidationHistory;
import com.marketmind.sources.domain.ValidationStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcSourceRegistryRepository implements SourceRegistryRepository {

    private static final String SOURCE_COLUMNS = """
            id, code, name, description, source_type, status, authentication_type,
            organization, refresh_frequency, base_url, robots_url, documentation_url,
            sample_pdf_url, enabled, priority, reliability_score, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSourceRegistryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SourceRegistry> findAllSources() {
        return jdbcTemplate.query(
                "SELECT " + SOURCE_COLUMNS + " FROM source_registry ORDER BY name",
                (resultSet, rowNum) -> mapSource(resultSet));
    }

    @Override
    public Optional<SourceRegistry> findSourceById(UUID id) {
        return jdbcTemplate.query(
                        "SELECT " + SOURCE_COLUMNS + " FROM source_registry WHERE id = ?",
                        (resultSet, rowNum) -> mapSource(resultSet),
                        id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsSourceByCode(String code, UUID excludedId) {
        Integer count = excludedId == null
                ? jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM source_registry WHERE UPPER(code) = UPPER(?)",
                        Integer.class,
                        code)
                : jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) FROM source_registry
                        WHERE UPPER(code) = UPPER(?) AND id <> ?
                        """,
                        Integer.class,
                        code,
                        excludedId);
        return count != null && count > 0;
    }

    @Override
    public SourceRegistry saveSource(SourceRegistry source) {
        jdbcTemplate.update(
                """
                INSERT INTO source_registry (
                    id, code, name, organization, description, source_type, status,
                    authentication_type, refresh_frequency, base_url, robots_url,
                    documentation_url, sample_pdf_url, enabled, priority,
                    reliability_score, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    code = EXCLUDED.code,
                    name = EXCLUDED.name,
                    organization = EXCLUDED.organization,
                    description = EXCLUDED.description,
                    source_type = EXCLUDED.source_type,
                    status = EXCLUDED.status,
                    authentication_type = EXCLUDED.authentication_type,
                    refresh_frequency = EXCLUDED.refresh_frequency,
                    base_url = EXCLUDED.base_url,
                    robots_url = EXCLUDED.robots_url,
                    documentation_url = EXCLUDED.documentation_url,
                    sample_pdf_url = EXCLUDED.sample_pdf_url,
                    enabled = EXCLUDED.enabled,
                    priority = EXCLUDED.priority,
                    reliability_score = EXCLUDED.reliability_score,
                    updated_at = EXCLUDED.updated_at
                """,
                source.id(),
                source.code(),
                source.name(),
                source.organization(),
                source.description(),
                source.sourceType().name(),
                source.status().name(),
                source.authenticationType().name(),
                source.refreshFrequency().name(),
                source.baseUrl().toString(),
                source.robotsUrl() == null ? null : source.robotsUrl().toString(),
                source.documentationUrl() == null ? null : source.documentationUrl().toString(),
                source.samplePdfUrl() == null ? null : source.samplePdfUrl().toString(),
                source.enabled(),
                source.priority(),
                source.reliabilityScore(),
                Timestamp.from(source.createdAt()),
                Timestamp.from(source.updatedAt()));
        return source;
    }

    @Override
    public void deleteSource(UUID id) {
        jdbcTemplate.update("DELETE FROM source_registry WHERE id = ?", id);
    }

    @Override
    public List<SourceHealth> findAllHealth() {
        return jdbcTemplate.query(
                """
                SELECT id, source_id, status, available, latency_ms, message, checked_at,
                       last_http_status, last_latency_ms, robots_txt_available,
                       robots_txt_status, pdf_capability_status, last_validated_at, created_at
                FROM source_health
                ORDER BY checked_at DESC
                """,
                (resultSet, rowNum) -> new SourceHealth(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("source_id", UUID.class),
                        SourceStatus.valueOf(resultSet.getString("status")),
                        resultSet.getBoolean("available"),
                        resultSet.getLong("latency_ms"),
                        resultSet.getString("message"),
                        instant(resultSet, "checked_at"),
                        integerNullable(resultSet, "last_http_status"),
                        longNullable(resultSet, "last_latency_ms"),
                        booleanNullable(resultSet, "robots_txt_available"),
                        integerNullable(resultSet, "robots_txt_status"),
                        enumNullable(
                                resultSet.getString("pdf_capability_status"),
                                CapabilityStatus.class),
                        instantNullable(resultSet, "last_validated_at"),
                        instant(resultSet, "created_at")));
    }

    @Override
    public SourceHealth saveHealth(SourceHealth health) {
        jdbcTemplate.update(
                """
                INSERT INTO source_health (
                    id, source_id, status, available, latency_ms, message, checked_at, created_at
                    , last_http_status, last_latency_ms, robots_txt_available,
                    robots_txt_status, pdf_capability_status, last_validated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                health.id(),
                health.sourceId(),
                health.status().name(),
                health.available(),
                health.latencyMs(),
                health.message(),
                Timestamp.from(health.checkedAt()),
                Timestamp.from(health.createdAt()),
                health.lastHttpStatus(),
                health.lastLatencyMs(),
                health.robotsTxtAvailable(),
                health.robotsTxtStatus(),
                health.pdfCapabilityStatus() == null ? null : health.pdfCapabilityStatus().name(),
                timestamp(health.lastValidatedAt()));
        return health;
    }

    @Override
    public List<SourceCapability> findAllCapabilities() {
        return jdbcTemplate.query(
                """
                SELECT id, source_id, capability_type, supported, verified_at, created_at
                FROM source_capability
                ORDER BY capability_type, source_id
                """,
                (resultSet, rowNum) -> mapCapability(resultSet));
    }

    @Override
    @Transactional
    public void replaceCapabilities(UUID sourceId, List<SourceCapability> capabilities) {
        jdbcTemplate.update("DELETE FROM source_capability WHERE source_id = ?", sourceId);
        capabilities.forEach(capability -> jdbcTemplate.update(
                """
                INSERT INTO source_capability (
                    id, source_id, capability_type, supported, verified_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                capability.id(),
                capability.sourceId(),
                capability.capabilityType().name(),
                capability.supported(),
                timestamp(capability.verifiedAt()),
                Timestamp.from(capability.createdAt())));
    }

    @Override
    public SourceValidationHistory saveValidation(SourceValidationHistory validation) {
        jdbcTemplate.update(
                """
                INSERT INTO source_validation_history (
                    id, source_id, validation_status, available, latency_ms, message,
                    supported_capabilities, validated_at, created_at, http_status,
                    robots_txt_available, robots_txt_status, pdf_capability_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                """,
                validation.id(),
                validation.sourceId(),
                validation.validationStatus().name(),
                validation.reachable(),
                validation.latencyMs(),
                validation.message(),
                capabilitiesJson(validation.supportedCapabilities()),
                Timestamp.from(validation.validatedAt()),
                Timestamp.from(validation.createdAt()),
                validation.httpStatus(),
                validation.robotsTxtAvailable(),
                validation.robotsTxtStatus(),
                validation.pdfCapabilityStatus().name());
        return validation;
    }

    private SourceRegistry mapSource(ResultSet resultSet) throws SQLException {
        UUID sourceId = resultSet.getObject("id", UUID.class);
        return new SourceRegistry(
                sourceId,
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("organization"),
                resultSet.getString("description"),
                SourceType.valueOf(resultSet.getString("source_type")),
                SourceStatus.valueOf(resultSet.getString("status")),
                AuthenticationType.valueOf(resultSet.getString("authentication_type")),
                RefreshFrequency.valueOf(resultSet.getString("refresh_frequency")),
                URI.create(resultSet.getString("base_url")),
                nullableUri(resultSet.getString("robots_url")),
                nullableUri(resultSet.getString("documentation_url")),
                nullableUri(resultSet.getString("sample_pdf_url")),
                findCapabilities(sourceId),
                resultSet.getBoolean("enabled"),
                resultSet.getInt("priority"),
                resultSet.getBigDecimal("reliability_score"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private Set<CapabilityType> findCapabilities(UUID sourceId) {
        return jdbcTemplate.query(
                        """
                        SELECT capability_type FROM source_capability
                        WHERE source_id = ? AND supported = TRUE
                        """,
                        (resultSet, rowNum) -> CapabilityType.valueOf(
                                resultSet.getString("capability_type")),
                        sourceId)
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private SourceCapability mapCapability(ResultSet resultSet) throws SQLException {
        return new SourceCapability(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("source_id", UUID.class),
                CapabilityType.valueOf(resultSet.getString("capability_type")),
                resultSet.getBoolean("supported"),
                instantNullable(resultSet, "verified_at"),
                instant(resultSet, "created_at"));
    }

    private String capabilitiesJson(Set<CapabilityType> capabilities) {
        try {
            return objectMapper.writeValueAsString(capabilities.stream()
                    .map(Enum::name)
                    .sorted()
                    .toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize source capabilities.", exception);
        }
    }

    private URI nullableUri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value);
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getTimestamp(column).toInstant();
    }

    private Instant instantNullable(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Integer integerNullable(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Long longNullable(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Boolean booleanNullable(ResultSet resultSet, String column) throws SQLException {
        boolean value = resultSet.getBoolean(column);
        return resultSet.wasNull() ? null : value;
    }

    private <T extends Enum<T>> T enumNullable(String value, Class<T> enumType) {
        return value == null ? null : Enum.valueOf(enumType, value);
    }
}
