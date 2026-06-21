package com.marketmind.ai.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.ai.application.ExtractedDocument;
import com.marketmind.ai.application.RagRepository;
import com.marketmind.ai.domain.AiAnswerStatus;
import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.Citation;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcRagRepository implements RagRepository {

    private static final TypeReference<List<Citation>> CITATIONS_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcRagRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ExtractedDocument> findCompletedExtraction(UUID documentId) {
        return jdbcTemplate.query("""
                SELECT e.document_id, e.document_version_id, d.title, e.extracted_text
                FROM document_text_extraction e
                JOIN document d ON d.id = e.document_id
                WHERE e.document_id = ?
                  AND e.extraction_status = 'COMPLETED'
                  AND e.extracted_text IS NOT NULL
                ORDER BY e.extracted_at DESC
                LIMIT 1
                """, (resultSet, rowNumber) -> new ExtractedDocument(
                resultSet.getObject("document_id", UUID.class),
                resultSet.getObject("document_version_id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("extracted_text")), documentId).stream().findFirst();
    }

    @Override
    @Transactional
    public void replaceChunks(UUID documentVersionId, List<DocumentChunk> chunks) {
        jdbcTemplate.update(
                "DELETE FROM document_chunk WHERE document_version_id = ?",
                documentVersionId);
        jdbcTemplate.batchUpdate("""
                INSERT INTO document_chunk (
                    id, document_id, document_version_id, chunk_index, chunk_text,
                    token_count, character_count, qdrant_collection,
                    qdrant_point_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, chunks, Math.min(chunks.size(), 200), (statement, chunk) -> {
            statement.setObject(1, chunk.id());
            statement.setObject(2, chunk.documentId());
            statement.setObject(3, chunk.documentVersionId());
            statement.setInt(4, chunk.chunkIndex());
            statement.setString(5, chunk.chunkText());
            statement.setInt(6, chunk.tokenCount());
            statement.setInt(7, chunk.characterCount());
            statement.setString(8, chunk.qdrantCollection());
            statement.setObject(9, chunk.qdrantPointId());
            statement.setTimestamp(10, timestamp(chunk.createdAt()));
        });
    }

    @Override
    public DocumentChunk updateVectorReference(
            UUID chunkId,
            String collectionName,
            UUID pointId) {
        jdbcTemplate.update("""
                UPDATE document_chunk
                SET qdrant_collection = ?, qdrant_point_id = ?
                WHERE id = ?
                """, collectionName, pointId, chunkId);
        return findChunk(chunkId).orElseThrow();
    }

    @Override
    public List<DocumentChunk> findChunks(UUID documentId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, document_version_id, chunk_index, chunk_text,
                       token_count, character_count, qdrant_collection,
                       qdrant_point_id, created_at
                FROM document_chunk
                WHERE document_id = ?
                ORDER BY chunk_index
                """, this::mapChunk, documentId);
    }

    @Override
    public DocumentEmbeddingJob saveEmbeddingJob(DocumentEmbeddingJob job) {
        jdbcTemplate.update("""
                INSERT INTO document_embedding_job (
                    id, document_id, document_version_id, status, total_chunks,
                    embedded_chunks, failed_chunks, error_message,
                    started_at, completed_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    total_chunks = EXCLUDED.total_chunks,
                    embedded_chunks = EXCLUDED.embedded_chunks,
                    failed_chunks = EXCLUDED.failed_chunks,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at
                """,
                job.id(), job.documentId(), job.documentVersionId(), job.status().name(),
                job.totalChunks(), job.embeddedChunks(), job.failedChunks(),
                job.errorMessage(), timestamp(job.startedAt()),
                timestamp(job.completedAt()), timestamp(job.createdAt()));
        return job;
    }

    @Override
    public AiQuestionAnswer saveAnswer(AiQuestionAnswer answer) {
        jdbcTemplate.update("""
                INSERT INTO ai_question_answer (
                    id, question, answer, document_id, status, citations,
                    confidence_score, created_at
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                """,
                answer.id(), answer.question(), answer.answer(), answer.documentId(),
                answer.status().name(), serialize(answer.citations()),
                answer.confidenceScore(), timestamp(answer.createdAt()));
        return answer;
    }

    @Override
    public List<AiQuestionAnswer> findRecentAnswers(int limit) {
        return jdbcTemplate.query("""
                SELECT id, question, answer, document_id, status,
                       citations::TEXT, confidence_score, created_at
                FROM ai_question_answer
                ORDER BY created_at DESC
                LIMIT ?
                """, this::mapAnswer, limit);
    }

    private Optional<DocumentChunk> findChunk(UUID id) {
        return jdbcTemplate.query("""
                SELECT id, document_id, document_version_id, chunk_index, chunk_text,
                       token_count, character_count, qdrant_collection,
                       qdrant_point_id, created_at
                FROM document_chunk WHERE id = ?
                """, this::mapChunk, id).stream().findFirst();
    }

    private DocumentChunk mapChunk(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DocumentChunk(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getObject("document_version_id", UUID.class),
                resultSet.getInt("chunk_index"),
                resultSet.getString("chunk_text"),
                resultSet.getInt("token_count"),
                resultSet.getInt("character_count"),
                resultSet.getString("qdrant_collection"),
                resultSet.getObject("qdrant_point_id", UUID.class),
                instant(resultSet, "created_at"));
    }

    private AiQuestionAnswer mapAnswer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AiQuestionAnswer(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("question"),
                resultSet.getString("answer"),
                resultSet.getObject("document_id", UUID.class),
                AiAnswerStatus.valueOf(resultSet.getString("status")),
                deserialize(resultSet.getString("citations")),
                resultSet.getBigDecimal("confidence_score"),
                instant(resultSet, "created_at"));
    }

    private String serialize(List<Citation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize citations.", exception);
        }
    }

    private List<Citation> deserialize(String value) {
        try {
            return objectMapper.readValue(value, CITATIONS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize citations.", exception);
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
