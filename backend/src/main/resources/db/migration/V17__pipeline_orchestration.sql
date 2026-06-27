CREATE TABLE pipeline_job (
    id UUID PRIMARY KEY,
    discovered_document_id UUID,
    document_id UUID,
    correlation_id VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_stage VARCHAR(40),
    progress_percent INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pipeline_job_discovered_document_fk
        FOREIGN KEY (discovered_document_id)
        REFERENCES discovered_document (id) ON DELETE SET NULL,
    CONSTRAINT pipeline_job_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE SET NULL,
    CONSTRAINT pipeline_job_input_check CHECK (
        discovered_document_id IS NOT NULL OR document_id IS NOT NULL
    ),
    CONSTRAINT pipeline_job_status_check CHECK (
        status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL')
    ),
    CONSTRAINT pipeline_job_stage_check CHECK (
        current_stage IS NULL OR current_stage IN (
            'DISCOVERY', 'DOWNLOAD', 'TEXT_EXTRACTION', 'CHUNKING',
            'EMBEDDING', 'QDRANT_INDEXING', 'AI_SUMMARY', 'AI_READY'
        )
    ),
    CONSTRAINT pipeline_job_progress_check
        CHECK (progress_percent BETWEEN 0 AND 100)
);

CREATE INDEX pipeline_job_created_idx ON pipeline_job (created_at DESC);
CREATE INDEX pipeline_job_status_idx ON pipeline_job (status, created_at DESC);
CREATE INDEX pipeline_job_document_idx ON pipeline_job (document_id, created_at DESC);
CREATE INDEX pipeline_job_discovered_idx
    ON pipeline_job (discovered_document_id, created_at DESC);
CREATE INDEX pipeline_job_correlation_idx ON pipeline_job (correlation_id);

CREATE TABLE pipeline_stage (
    id UUID PRIMARY KEY,
    pipeline_job_id UUID NOT NULL,
    stage_name VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pipeline_stage_job_fk
        FOREIGN KEY (pipeline_job_id) REFERENCES pipeline_job (id) ON DELETE CASCADE,
    CONSTRAINT pipeline_stage_name_check CHECK (
        stage_name IN (
            'DISCOVERY', 'DOWNLOAD', 'TEXT_EXTRACTION', 'CHUNKING',
            'EMBEDDING', 'QDRANT_INDEXING', 'AI_SUMMARY', 'AI_READY'
        )
    ),
    CONSTRAINT pipeline_stage_status_check CHECK (
        status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED')
    ),
    CONSTRAINT pipeline_stage_attempt_check CHECK (
        attempt_count >= 0 AND max_attempts BETWEEN 1 AND 10
        AND attempt_count <= max_attempts
    ),
    CONSTRAINT pipeline_stage_duration_check CHECK (duration_ms >= 0),
    CONSTRAINT pipeline_stage_job_name_uk UNIQUE (pipeline_job_id, stage_name)
);

CREATE INDEX pipeline_stage_job_idx
    ON pipeline_stage (pipeline_job_id, created_at);
CREATE INDEX pipeline_stage_status_idx
    ON pipeline_stage (status, stage_name);

CREATE TABLE pipeline_event (
    id UUID PRIMARY KEY,
    pipeline_job_id UUID NOT NULL,
    pipeline_stage_id UUID,
    event_type VARCHAR(40) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pipeline_event_job_fk
        FOREIGN KEY (pipeline_job_id) REFERENCES pipeline_job (id) ON DELETE CASCADE,
    CONSTRAINT pipeline_event_stage_fk
        FOREIGN KEY (pipeline_stage_id) REFERENCES pipeline_stage (id) ON DELETE SET NULL,
    CONSTRAINT pipeline_event_type_check CHECK (
        event_type IN (
            'JOB_CREATED', 'JOB_STARTED', 'STAGE_STARTED', 'STAGE_RETRYING',
            'STAGE_COMPLETED', 'STAGE_FAILED', 'STAGE_SKIPPED',
            'SUMMARY_GENERATED', 'JOB_COMPLETED', 'JOB_FAILED'
        )
    )
);

CREATE INDEX pipeline_event_job_idx
    ON pipeline_event (pipeline_job_id, created_at);
