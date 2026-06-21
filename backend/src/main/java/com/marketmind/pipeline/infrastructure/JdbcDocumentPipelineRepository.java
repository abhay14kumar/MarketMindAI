package com.marketmind.pipeline.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.pipeline.application.DocumentPipelineRepository;
import com.marketmind.pipeline.application.PageResult;
import com.marketmind.pipeline.application.PipelineRunSummary;
import com.marketmind.pipeline.domain.DocumentPipelineRun;
import com.marketmind.pipeline.domain.DocumentPipelineStep;
import com.marketmind.pipeline.domain.PipelineStatus;
import com.marketmind.pipeline.domain.PipelineStepName;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDocumentPipelineRepository implements DocumentPipelineRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentPipelineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DocumentPipelineRun saveRun(DocumentPipelineRun run) {
        jdbcTemplate.update("""
                INSERT INTO document_pipeline_run (
                    id, document_id, status, current_step, started_at,
                    completed_at, error_message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    current_step = EXCLUDED.current_step,
                    completed_at = EXCLUDED.completed_at,
                    error_message = EXCLUDED.error_message
                """,
                run.id(),
                run.documentId(),
                run.status().name(),
                run.currentStep().name(),
                timestamp(run.startedAt()),
                timestamp(run.completedAt()),
                run.errorMessage(),
                timestamp(run.createdAt()));
        return run;
    }

    @Override
    public DocumentPipelineStep saveStep(DocumentPipelineStep step) {
        jdbcTemplate.update("""
                INSERT INTO document_pipeline_step (
                    id, pipeline_run_id, document_id, step_name, status,
                    started_at, completed_at, error_message, retry_count, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    completed_at = EXCLUDED.completed_at,
                    error_message = EXCLUDED.error_message,
                    retry_count = EXCLUDED.retry_count
                """,
                step.id(),
                step.pipelineRunId(),
                step.documentId(),
                step.stepName().name(),
                step.status().name(),
                timestamp(step.startedAt()),
                timestamp(step.completedAt()),
                step.errorMessage(),
                step.retryCount(),
                timestamp(step.createdAt()));
        return step;
    }

    @Override
    public PageResult<PipelineRunSummary> findRuns(int page, int size) {
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_pipeline_run",
                Long.class);
        List<PipelineRunSummary> content = jdbcTemplate.query("""
                SELECT r.id, r.document_id, r.status, r.current_step,
                       r.started_at, r.completed_at, r.error_message, r.created_at,
                       d.title AS document_title
                FROM document_pipeline_run r
                JOIN document d ON d.id = r.document_id
                ORDER BY r.created_at DESC
                LIMIT ? OFFSET ?
                """, this::mapSummary, size, (long) page * size);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    @Override
    public Optional<PipelineRunSummary> findRun(UUID runId) {
        return jdbcTemplate.query("""
                SELECT r.id, r.document_id, r.status, r.current_step,
                       r.started_at, r.completed_at, r.error_message, r.created_at,
                       d.title AS document_title
                FROM document_pipeline_run r
                JOIN document d ON d.id = r.document_id
                WHERE r.id = ?
                """, this::mapSummary, runId).stream().findFirst();
    }

    @Override
    public Optional<PipelineRunSummary> findLatestRun(UUID documentId) {
        return jdbcTemplate.query("""
                SELECT r.id, r.document_id, r.status, r.current_step,
                       r.started_at, r.completed_at, r.error_message, r.created_at,
                       d.title AS document_title
                FROM document_pipeline_run r
                JOIN document d ON d.id = r.document_id
                WHERE r.document_id = ?
                ORDER BY r.created_at DESC
                LIMIT 1
                """, this::mapSummary, documentId).stream().findFirst();
    }

    @Override
    public List<DocumentPipelineStep> findSteps(UUID runId) {
        return jdbcTemplate.query("""
                SELECT id, pipeline_run_id, document_id, step_name, status,
                       started_at, completed_at, error_message, retry_count, created_at
                FROM document_pipeline_step
                WHERE pipeline_run_id = ?
                ORDER BY created_at, step_name
                """, this::mapStep, runId);
    }

    @Override
    public int nextRetryCount(UUID documentId, PipelineStepName stepName) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(retry_count), -1) + 1
                FROM document_pipeline_step
                WHERE document_id = ? AND step_name = ?
                """, Integer.class, documentId, stepName.name());
        return value == null ? 0 : value;
    }

    private PipelineRunSummary mapSummary(
            ResultSet resultSet,
            int rowNumber) throws SQLException {
        return new PipelineRunSummary(
                new DocumentPipelineRun(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("document_id", UUID.class),
                        PipelineStatus.valueOf(resultSet.getString("status")),
                        PipelineStepName.valueOf(resultSet.getString("current_step")),
                        instant(resultSet, "started_at"),
                        instant(resultSet, "completed_at"),
                        resultSet.getString("error_message"),
                        instant(resultSet, "created_at")),
                resultSet.getString("document_title"));
    }

    private DocumentPipelineStep mapStep(
            ResultSet resultSet,
            int rowNumber) throws SQLException {
        return new DocumentPipelineStep(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("pipeline_run_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                PipelineStepName.valueOf(resultSet.getString("step_name")),
                PipelineStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                resultSet.getString("error_message"),
                resultSet.getInt("retry_count"),
                instant(resultSet, "created_at"));
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
