ALTER TABLE documents RENAME TO document;

ALTER TABLE document RENAME CONSTRAINT documents_pkey TO document_pkey;
ALTER TABLE document RENAME CONSTRAINT documents_company_fk TO document_company_fk;
ALTER TABLE document RENAME CONSTRAINT documents_checksum_uk TO document_checksum_uk;
ALTER TABLE document RENAME CONSTRAINT documents_type_check TO document_type_check;
ALTER INDEX documents_company_publication_date_idx
    RENAME TO document_company_publication_date_idx;
ALTER INDEX documents_ingestion_status_idx RENAME TO document_ingestion_status_idx;

CREATE TABLE document_source (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    base_url TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_source_code_uk UNIQUE (code),
    CONSTRAINT document_source_code_format_check
        CHECK (code ~ '^[A-Z0-9][A-Z0-9._-]{0,63}$'),
    CONSTRAINT document_source_type_check CHECK (
        source_type IN ('EXCHANGE', 'REGULATOR', 'COMPANY_INVESTOR_RELATIONS', 'MANUAL')
    ),
    CONSTRAINT document_source_base_url_check
        CHECK (base_url ~ '^https://[^[:space:]]+$')
);

CREATE INDEX document_source_enabled_idx ON document_source (enabled);

ALTER TABLE document
    ADD COLUMN source_id UUID,
    ADD COLUMN current_version_id UUID,
    ADD COLUMN discovered_at TIMESTAMPTZ,
    ADD COLUMN last_acquired_at TIMESTAMPTZ;

INSERT INTO document_source (
    id,
    code,
    name,
    source_type,
    base_url,
    enabled
)
SELECT DISTINCT
    MD5(LOWER(source))::UUID,
    'LEGACY-' || UPPER(SUBSTRING(MD5(LOWER(source)), 1, 12)),
    source,
    'MANUAL',
    'https://legacy.invalid/' || LOWER(SUBSTRING(MD5(LOWER(source)), 1, 16)),
    FALSE
FROM document
WHERE source IS NOT NULL;

UPDATE document
SET source_id = MD5(LOWER(source))::UUID,
    discovered_at = created_at,
    last_acquired_at = CASE
        WHEN ingestion_status = 'COMPLETED' THEN updated_at
        ELSE NULL
    END
WHERE source IS NOT NULL;

ALTER TABLE document
    ADD CONSTRAINT document_source_fk
        FOREIGN KEY (source_id) REFERENCES document_source (id) ON DELETE RESTRICT,
    ALTER COLUMN source DROP NOT NULL,
    ALTER COLUMN checksum_sha256 DROP NOT NULL;

ALTER TABLE document DROP CONSTRAINT documents_ingestion_status_check;
ALTER TABLE document RENAME COLUMN ingestion_status TO status;
ALTER INDEX document_ingestion_status_idx RENAME TO document_status_idx;

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
            'FAILED',
            'ARCHIVED'
        )
    ),
    ADD CONSTRAINT document_source_url_check
        CHECK (source_url IS NULL OR source_url ~ '^https://[^[:space:]]+$');

CREATE INDEX document_source_status_idx ON document (source_id, status);

CREATE TABLE document_version (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    storage_reference TEXT,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_version_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE,
    CONSTRAINT document_version_number_uk UNIQUE (document_id, version_number),
    CONSTRAINT document_version_checksum_uk UNIQUE (document_id, checksum_sha256),
    CONSTRAINT document_version_number_check CHECK (version_number > 0),
    CONSTRAINT document_version_checksum_format_check
        CHECK (checksum_sha256 ~ '^[a-fA-F0-9]{64}$'),
    CONSTRAINT document_version_size_check CHECK (size_bytes >= 0)
);

CREATE INDEX document_version_document_acquired_idx
    ON document_version (document_id, acquired_at DESC);
CREATE INDEX document_version_checksum_idx ON document_version (checksum_sha256);

INSERT INTO document_version (
    id,
    document_id,
    version_number,
    checksum_sha256,
    storage_reference,
    mime_type,
    size_bytes,
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
    COALESCE(last_acquired_at, updated_at, created_at),
    created_at
FROM document
WHERE checksum_sha256 IS NOT NULL;

UPDATE document
SET current_version_id = document_version.id
FROM document_version
WHERE document_version.document_id = document.id
  AND document_version.version_number = 1;

ALTER TABLE document
    ADD CONSTRAINT document_current_version_fk
        FOREIGN KEY (current_version_id) REFERENCES document_version (id) ON DELETE SET NULL;

CREATE INDEX document_current_version_idx ON document (current_version_id);

CREATE TABLE download_job (
    id UUID PRIMARY KEY,
    document_id UUID,
    source_id UUID NOT NULL,
    requested_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    retry_of_job_id UUID,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT download_job_document_fk
        FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE SET NULL,
    CONSTRAINT download_job_source_fk
        FOREIGN KEY (source_id) REFERENCES document_source (id) ON DELETE RESTRICT,
    CONSTRAINT download_job_retry_fk
        FOREIGN KEY (retry_of_job_id) REFERENCES download_job (id) ON DELETE SET NULL,
    CONSTRAINT download_job_url_check
        CHECK (requested_url ~ '^https://[^[:space:]]+$'),
    CONSTRAINT download_job_status_check CHECK (
        status IN ('QUEUED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT download_job_attempt_count_check CHECK (attempt_count >= 0),
    CONSTRAINT download_job_max_attempts_check CHECK (max_attempts BETWEEN 1 AND 10),
    CONSTRAINT download_job_attempt_bound_check CHECK (attempt_count <= max_attempts),
    CONSTRAINT download_job_completion_check CHECK (
        completed_at IS NULL OR status IN ('COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX download_job_status_next_attempt_idx
    ON download_job (status, next_attempt_at);
CREATE INDEX download_job_document_submitted_idx
    ON download_job (document_id, submitted_at DESC);
CREATE INDEX download_job_source_submitted_idx
    ON download_job (source_id, submitted_at DESC);
