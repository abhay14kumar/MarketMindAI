CREATE TABLE document_sources (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    base_url TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_sources_code_uk UNIQUE (code),
    CONSTRAINT document_sources_type_check CHECK (
        source_type IN ('EXCHANGE', 'REGULATOR', 'COMPANY', 'NEWSWIRE', 'MANUAL', 'OTHER')
    ),
    CONSTRAINT document_sources_base_url_check CHECK (
        base_url IS NULL OR base_url ~ '^https://[^[:space:]]+$'
    )
);

CREATE INDEX document_sources_enabled_idx ON document_sources (enabled);

ALTER TABLE documents
    ADD COLUMN source_id UUID,
    ADD COLUMN discovered_at TIMESTAMPTZ,
    ADD COLUMN last_acquired_at TIMESTAMPTZ;

INSERT INTO document_sources (
    id,
    code,
    name,
    source_type,
    enabled,
    created_at,
    updated_at
)
SELECT DISTINCT
    MD5(LOWER(source))::UUID,
    'LEGACY-' || UPPER(SUBSTRING(MD5(LOWER(source)), 1, 12)),
    source,
    'OTHER',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM documents
WHERE source IS NOT NULL;

UPDATE documents
SET source_id = MD5(LOWER(source))::UUID,
    discovered_at = created_at,
    last_acquired_at = CASE
        WHEN ingestion_status = 'COMPLETED' THEN updated_at
        ELSE NULL
    END
WHERE source IS NOT NULL;

ALTER TABLE documents
    ADD CONSTRAINT documents_source_fk
        FOREIGN KEY (source_id) REFERENCES document_sources (id) ON DELETE RESTRICT;

ALTER TABLE documents
    ALTER COLUMN source DROP NOT NULL,
    ALTER COLUMN checksum_sha256 DROP NOT NULL;

ALTER TABLE documents DROP CONSTRAINT documents_ingestion_status_check;
ALTER TABLE documents
    ADD CONSTRAINT documents_ingestion_status_check CHECK (
        ingestion_status IN (
            'PENDING',
            'DISCOVERED',
            'DOWNLOAD_QUEUED',
            'DOWNLOADED',
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        )
    ),
    ADD CONSTRAINT documents_source_url_check CHECK (
        source_url IS NULL OR source_url ~ '^https://[^[:space:]]+$'
    );

CREATE INDEX documents_source_status_idx
    ON documents (source_id, ingestion_status);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    storage_reference TEXT,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    parse_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    acquired_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_versions_document_fk
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT document_versions_number_uk UNIQUE (document_id, version_number),
    CONSTRAINT document_versions_checksum_uk UNIQUE (document_id, checksum_sha256),
    CONSTRAINT document_versions_version_check CHECK (version_number > 0),
    CONSTRAINT document_versions_checksum_format_check CHECK (
        checksum_sha256 ~ '^[a-fA-F0-9]{64}$'
    ),
    CONSTRAINT document_versions_size_check CHECK (size_bytes >= 0),
    CONSTRAINT document_versions_parse_status_check CHECK (
        parse_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX document_versions_document_acquired_idx
    ON document_versions (document_id, acquired_at DESC);
CREATE INDEX document_versions_checksum_idx
    ON document_versions (checksum_sha256);
CREATE INDEX document_versions_parse_status_idx
    ON document_versions (parse_status);

INSERT INTO document_versions (
    id,
    document_id,
    version_number,
    checksum_sha256,
    storage_reference,
    mime_type,
    size_bytes,
    parse_status,
    acquired_at,
    created_at
)
SELECT
    MD5(id::TEXT || checksum_sha256)::UUID,
    id,
    1,
    checksum_sha256,
    storage_reference,
    'application/octet-stream',
    0,
    CASE ingestion_status
        WHEN 'PROCESSING' THEN 'PROCESSING'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        WHEN 'FAILED' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    COALESCE(last_acquired_at, updated_at, created_at),
    created_at
FROM documents
WHERE checksum_sha256 IS NOT NULL;

ALTER TABLE documents ADD COLUMN current_version_id UUID;

UPDATE documents
SET current_version_id = document_versions.id
FROM document_versions
WHERE document_versions.document_id = documents.id
  AND document_versions.version_number = 1;

ALTER TABLE documents
    ADD CONSTRAINT documents_current_version_fk
        FOREIGN KEY (current_version_id) REFERENCES document_versions (id) ON DELETE SET NULL;

CREATE INDEX documents_current_version_idx ON documents (current_version_id);

CREATE TABLE download_jobs (
    id UUID PRIMARY KEY,
    document_id UUID,
    source_id UUID NOT NULL,
    requested_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT download_jobs_document_fk
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE SET NULL,
    CONSTRAINT download_jobs_source_fk
        FOREIGN KEY (source_id) REFERENCES document_sources (id) ON DELETE RESTRICT,
    CONSTRAINT download_jobs_url_check CHECK (
        requested_url ~ '^https://[^[:space:]]+$'
    ),
    CONSTRAINT download_jobs_status_check CHECK (
        status IN ('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT download_jobs_attempt_count_check CHECK (attempt_count >= 0),
    CONSTRAINT download_jobs_max_attempts_check CHECK (
        max_attempts BETWEEN 1 AND 10
    ),
    CONSTRAINT download_jobs_attempt_bound_check CHECK (attempt_count <= max_attempts),
    CONSTRAINT download_jobs_completion_check CHECK (
        completed_at IS NULL OR status IN ('COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX download_jobs_status_next_attempt_idx
    ON download_jobs (status, next_attempt_at);
CREATE INDEX download_jobs_document_submitted_idx
    ON download_jobs (document_id, submitted_at DESC);
CREATE INDEX download_jobs_source_submitted_idx
    ON download_jobs (source_id, submitted_at DESC);
