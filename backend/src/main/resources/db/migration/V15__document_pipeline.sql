ALTER TABLE document DROP CONSTRAINT document_status_check;
ALTER TABLE document
    ADD CONSTRAINT document_status_check CHECK (
        status IN (
            'PENDING',
            'DISCOVERED',
            'QUEUED',
            'ACQUIRED',
            'PARSED',
            'PROCESSING',
            'COMPLETED',
            'AI_READY',
            'FAILED',
            'ARCHIVED'
        )
    );

CREATE TABLE document_pipeline_run (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_step VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_pipeline_run_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_pipeline_run_status_check
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED', 'SKIPPED', 'PARTIAL')),
    CONSTRAINT document_pipeline_run_step_check
        CHECK (current_step IN (
            'DOWNLOAD', 'TEXT_EXTRACTION', 'CHUNKING', 'EMBEDDING', 'AI_READY'
        ))
);

CREATE INDEX document_pipeline_run_created_idx
    ON document_pipeline_run (created_at DESC);
CREATE INDEX document_pipeline_run_document_idx
    ON document_pipeline_run (document_id, created_at DESC);
CREATE INDEX document_pipeline_run_status_idx
    ON document_pipeline_run (status, created_at DESC);

CREATE TABLE document_pipeline_step (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL,
    document_id UUID NOT NULL,
    step_name VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_pipeline_step_run_fk
        FOREIGN KEY (pipeline_run_id)
        REFERENCES document_pipeline_run (id) ON DELETE CASCADE,
    CONSTRAINT document_pipeline_step_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_pipeline_step_name_check
        CHECK (step_name IN (
            'DOWNLOAD', 'TEXT_EXTRACTION', 'CHUNKING', 'EMBEDDING', 'AI_READY'
        )),
    CONSTRAINT document_pipeline_step_status_check
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED', 'SKIPPED', 'PARTIAL')),
    CONSTRAINT document_pipeline_step_retry_check CHECK (retry_count >= 0),
    CONSTRAINT document_pipeline_step_run_name_uk UNIQUE (pipeline_run_id, step_name)
);

CREATE INDEX document_pipeline_step_run_idx
    ON document_pipeline_step (pipeline_run_id, created_at);
CREATE INDEX document_pipeline_step_document_idx
    ON document_pipeline_step (document_id, step_name, created_at DESC);
