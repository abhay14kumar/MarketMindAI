package com.marketmind.documents.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.documents.application.DocumentTextExtractionRepository;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.ExtractionStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDocumentTextExtractionRepository implements DocumentTextExtractionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentTextExtractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DocumentTextExtraction save(DocumentTextExtraction extraction) {
        jdbcTemplate.update("""
                INSERT INTO document_text_extraction (
                    id, document_id, document_version_id, extraction_status,
                    extracted_text, page_count, character_count, error_message,
                    extracted_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (document_version_id) DO UPDATE SET
                    id = EXCLUDED.id,
                    extraction_status = EXCLUDED.extraction_status,
                    extracted_text = EXCLUDED.extracted_text,
                    page_count = EXCLUDED.page_count,
                    character_count = EXCLUDED.character_count,
                    error_message = EXCLUDED.error_message,
                    extracted_at = EXCLUDED.extracted_at,
                    created_at = EXCLUDED.created_at
                """,
                extraction.id(), extraction.documentId(), extraction.documentVersionId(),
                extraction.extractionStatus().name(), extraction.extractedText(),
                extraction.pageCount(), extraction.characterCount(), extraction.errorMessage(),
                timestamp(extraction.extractedAt()), timestamp(extraction.createdAt()));
        return extraction;
    }

    @Override
    public Optional<DocumentTextExtraction> findLatestByDocumentId(UUID documentId) {
        return jdbcTemplate.query("""
                SELECT id, document_id, document_version_id, extraction_status,
                       extracted_text, page_count, character_count, error_message,
                       extracted_at, created_at
                FROM document_text_extraction
                WHERE document_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, this::map, documentId).stream().findFirst();
    }

    private DocumentTextExtraction map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DocumentTextExtraction(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getObject("document_version_id", UUID.class),
                ExtractionStatus.valueOf(resultSet.getString("extraction_status")),
                resultSet.getString("extracted_text"),
                nullableInteger(resultSet, "page_count"),
                nullableLong(resultSet, "character_count"),
                resultSet.getString("error_message"),
                instant(resultSet, "extracted_at"),
                instant(resultSet, "created_at"));
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
