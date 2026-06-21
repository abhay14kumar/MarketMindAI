CREATE TABLE document_chunk (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    character_count INTEGER NOT NULL,
    qdrant_collection VARCHAR(120),
    qdrant_point_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_chunk_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_chunk_version_fk
        FOREIGN KEY (document_version_id) REFERENCES document_version (id) ON DELETE CASCADE,
    CONSTRAINT document_chunk_index_uk UNIQUE (document_version_id, chunk_index),
    CONSTRAINT document_chunk_counts_check
        CHECK (chunk_index >= 0 AND token_count >= 0 AND character_count > 0)
);

CREATE INDEX document_chunk_document_idx
    ON document_chunk (document_id, chunk_index);

CREATE TABLE document_embedding_job (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    embedded_chunks INTEGER NOT NULL DEFAULT 0,
    failed_chunks INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_embedding_job_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_embedding_job_version_fk
        FOREIGN KEY (document_version_id) REFERENCES document_version (id) ON DELETE CASCADE,
    CONSTRAINT document_embedding_job_status_check
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED', 'PARTIAL')),
    CONSTRAINT document_embedding_job_counts_check CHECK (
        total_chunks >= 0 AND embedded_chunks >= 0 AND failed_chunks >= 0
        AND embedded_chunks + failed_chunks <= total_chunks
    )
);

CREATE INDEX document_embedding_job_document_idx
    ON document_embedding_job (document_id, started_at DESC);

CREATE TABLE ai_question_answer (
    id UUID PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    document_id UUID,
    status VARCHAR(30) NOT NULL,
    citations JSONB NOT NULL DEFAULT '[]'::JSONB,
    confidence_score NUMERIC(5, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ai_question_answer_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE SET NULL,
    CONSTRAINT ai_question_answer_status_check
        CHECK (status IN ('SUCCESS', 'FAILED', 'INSUFFICIENT_CONTEXT')),
    CONSTRAINT ai_question_answer_confidence_check
        CHECK (confidence_score >= 0 AND confidence_score <= 1),
    CONSTRAINT ai_question_answer_citations_check
        CHECK (JSONB_TYPEOF(citations) = 'array')
);

CREATE INDEX ai_question_answer_created_idx
    ON ai_question_answer (created_at DESC);
