package com.marketmind.pipeline.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.pipeline.application.PageResult;
import com.marketmind.pipeline.application.PipelineJobRepository;
import com.marketmind.pipeline.application.PipelineMetrics;
import com.marketmind.pipeline.domain.PipelineEvent;
import com.marketmind.pipeline.domain.PipelineEventType;
import com.marketmind.pipeline.domain.PipelineJob;
import com.marketmind.pipeline.domain.PipelineJobStatus;
import com.marketmind.pipeline.domain.PipelineStage;
import com.marketmind.pipeline.domain.PipelineStageName;
import com.marketmind.pipeline.domain.PipelineStageStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPipelineJobRepository implements PipelineJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPipelineJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PipelineJob saveJob(PipelineJob job) {
        jdbcTemplate.update("""
                INSERT INTO pipeline_job (
                    id, discovered_document_id, document_id, correlation_id,
                    status, current_stage, progress_percent, error_message,
                    started_at, completed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    document_id = EXCLUDED.document_id,
                    status = EXCLUDED.status,
                    current_stage = EXCLUDED.current_stage,
                    progress_percent = EXCLUDED.progress_percent,
                    error_message = EXCLUDED.error_message,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = EXCLUDED.updated_at
                """,
                job.id(), job.discoveredDocumentId(), job.documentId(),
                job.correlationId(), job.status().name(),
                name(job.currentStage()), job.progressPercent(), job.errorMessage(),
                timestamp(job.startedAt()), timestamp(job.completedAt()),
                timestamp(job.createdAt()), timestamp(job.updatedAt()));
        return job;
    }

    @Override
    public PipelineStage saveStage(PipelineStage stage) {
        jdbcTemplate.update("""
                INSERT INTO pipeline_stage (
                    id, pipeline_job_id, stage_name, status, attempt_count,
                    max_attempts, duration_ms, error_message, started_at,
                    completed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    attempt_count = EXCLUDED.attempt_count,
                    duration_ms = EXCLUDED.duration_ms,
                    error_message = EXCLUDED.error_message,
                    started_at = EXCLUDED.started_at,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = EXCLUDED.updated_at
                """,
                stage.id(), stage.pipelineJobId(), stage.stageName().name(),
                stage.status().name(), stage.attemptCount(), stage.maxAttempts(),
                stage.durationMs(), stage.errorMessage(), timestamp(stage.startedAt()),
                timestamp(stage.completedAt()), timestamp(stage.createdAt()),
                timestamp(stage.updatedAt()));
        return stage;
    }

    @Override
    public PipelineEvent saveEvent(PipelineEvent event) {
        jdbcTemplate.update("""
                INSERT INTO pipeline_event (
                    id, pipeline_job_id, pipeline_stage_id, event_type,
                    message, details, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(), event.pipelineJobId(), event.pipelineStageId(),
                event.eventType().name(), event.message(), event.details(),
                timestamp(event.createdAt()));
        return event;
    }

    @Override
    public Optional<PipelineJob> findJob(UUID jobId) {
        return jdbcTemplate.query(
                "SELECT * FROM pipeline_job WHERE id = ?",
                this::mapJob,
                jobId).stream().findFirst();
    }

    @Override
    public PageResult<PipelineJob> findJobs(int page, int size) {
        Long totalValue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pipeline_job", Long.class);
        long total = totalValue == null ? 0 : totalValue;
        List<PipelineJob> jobs = jdbcTemplate.query("""
                SELECT * FROM pipeline_job
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, this::mapJob, size, (long) page * size);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(jobs, page, size, total, totalPages);
    }

    @Override
    public List<PipelineStage> findStages(UUID jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM pipeline_stage
                WHERE pipeline_job_id = ?
                ORDER BY created_at, stage_name
                """, this::mapStage, jobId);
    }

    @Override
    public List<PipelineEvent> findEvents(UUID jobId) {
        return jdbcTemplate.query("""
                SELECT * FROM pipeline_event
                WHERE pipeline_job_id = ?
                ORDER BY created_at
                """, this::mapEvent, jobId);
    }

    @Override
    public PipelineMetrics metrics() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) AS total_jobs,
                    COUNT(*) FILTER (WHERE status IN ('QUEUED', 'RUNNING')) AS running_jobs,
                    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_jobs,
                    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_jobs,
                    COALESCE(AVG(
                        EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000
                    ) FILTER (
                        WHERE completed_at IS NOT NULL AND started_at IS NOT NULL
                    ), 0) AS average_duration_ms
                FROM pipeline_job
                """, (resultSet, row) -> {
            long total = resultSet.getLong("total_jobs");
            long completed = resultSet.getLong("completed_jobs");
            return new PipelineMetrics(
                    total,
                    resultSet.getLong("running_jobs"),
                    completed,
                    resultSet.getLong("failed_jobs"),
                    total == 0 ? 0 : (double) completed / total,
                    resultSet.getDouble("average_duration_ms"));
        });
    }

    private PipelineJob mapJob(ResultSet resultSet, int row) throws SQLException {
        return new PipelineJob(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("discovered_document_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("correlation_id"),
                PipelineJobStatus.valueOf(resultSet.getString("status")),
                enumValue(resultSet.getString("current_stage"), PipelineStageName.class),
                resultSet.getInt("progress_percent"),
                resultSet.getString("error_message"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private PipelineStage mapStage(ResultSet resultSet, int row) throws SQLException {
        return new PipelineStage(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("pipeline_job_id", UUID.class),
                PipelineStageName.valueOf(resultSet.getString("stage_name")),
                PipelineStageStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("attempt_count"),
                resultSet.getInt("max_attempts"),
                resultSet.getLong("duration_ms"),
                resultSet.getString("error_message"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private PipelineEvent mapEvent(ResultSet resultSet, int row) throws SQLException {
        return new PipelineEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("pipeline_job_id", UUID.class),
                resultSet.getObject("pipeline_stage_id", UUID.class),
                PipelineEventType.valueOf(resultSet.getString("event_type")),
                resultSet.getString("message"),
                resultSet.getString("details"),
                instant(resultSet, "created_at"));
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> type) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
