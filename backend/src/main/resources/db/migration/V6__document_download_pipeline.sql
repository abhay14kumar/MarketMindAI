ALTER TABLE document
    ADD COLUMN fiscal_year INTEGER,
    ADD COLUMN quarter VARCHAR(2);

ALTER TABLE document_version
    ALTER COLUMN checksum_sha256 TYPE VARCHAR(64);

ALTER TABLE document
    ADD CONSTRAINT document_fiscal_year_check CHECK (
        fiscal_year IS NULL OR fiscal_year BETWEEN 1900 AND 2200
    ),
    ADD CONSTRAINT document_quarter_check CHECK (
        quarter IS NULL OR quarter IN ('Q1', 'Q2', 'Q3', 'Q4')
    );

ALTER TABLE document DROP CONSTRAINT document_source_url_check;
ALTER TABLE document
    ADD CONSTRAINT document_source_url_check CHECK (
        source_url IS NULL OR source_url ~ '^https?://[^[:space:]]+$'
    );

CREATE INDEX document_fiscal_period_idx
    ON document (fiscal_year, quarter)
    WHERE fiscal_year IS NOT NULL;

CREATE UNIQUE INDEX document_version_checksum_uidx
    ON document_version (LOWER(checksum_sha256));

ALTER TABLE download_job DROP CONSTRAINT download_job_source_fk;
ALTER TABLE download_job ALTER COLUMN source_id DROP NOT NULL;
ALTER TABLE download_job
    ADD CONSTRAINT download_job_source_fk
        FOREIGN KEY (source_id) REFERENCES document_source (id) ON DELETE RESTRICT;

ALTER TABLE download_job DROP CONSTRAINT download_job_url_check;
ALTER TABLE download_job DROP CONSTRAINT download_job_status_check;

ALTER TABLE download_job
    ADD CONSTRAINT download_job_url_check CHECK (
        requested_url ~ '^https?://[^[:space:]]+$'
    ),
    ADD CONSTRAINT download_job_status_check CHECK (
        status IN ('QUEUED', 'STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')
    );
