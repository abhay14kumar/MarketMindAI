CREATE TABLE document_text_extraction (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    extraction_status VARCHAR(20) NOT NULL,
    extracted_text TEXT,
    page_count INTEGER,
    character_count BIGINT,
    error_message VARCHAR(1000),
    extracted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_text_extraction_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_text_extraction_version_fk
        FOREIGN KEY (document_version_id) REFERENCES document_version (id) ON DELETE CASCADE,
    CONSTRAINT document_text_extraction_status_check
        CHECK (extraction_status IN ('STARTED', 'COMPLETED', 'FAILED', 'UNSUPPORTED')),
    CONSTRAINT document_text_extraction_page_count_check
        CHECK (page_count IS NULL OR page_count >= 0),
    CONSTRAINT document_text_extraction_character_count_check
        CHECK (character_count IS NULL OR character_count >= 0),
    CONSTRAINT document_text_extraction_version_uk UNIQUE (document_version_id)
);

CREATE INDEX document_text_extraction_document_idx
    ON document_text_extraction (document_id, created_at DESC);
CREATE INDEX document_text_extraction_status_idx
    ON document_text_extraction (extraction_status);
