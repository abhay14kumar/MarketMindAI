CREATE TABLE discovery_job (
    id UUID PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL,
    source_url TEXT,
    status VARCHAR(20) NOT NULL,
    total_discovered INTEGER NOT NULL DEFAULT 0,
    new_documents INTEGER NOT NULL DEFAULT 0,
    existing_documents INTEGER NOT NULL DEFAULT 0,
    failed_sources INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT discovery_job_source_type_check CHECK (
        source_type IN ('NSE', 'BSE', 'SEBI', 'RBI', 'COMPANY_IR', 'TEST_SOURCE')
    ),
    CONSTRAINT discovery_job_status_check CHECK (
        status IN ('STARTED', 'COMPLETED', 'FAILED', 'PARTIAL')
    ),
    CONSTRAINT discovery_job_counts_check CHECK (
        total_discovered >= 0
        AND new_documents >= 0
        AND existing_documents >= 0
        AND failed_sources >= 0
        AND new_documents + existing_documents <= total_discovered
    )
);

CREATE INDEX discovery_job_created_idx ON discovery_job (created_at DESC);
CREATE INDEX discovery_job_status_idx ON discovery_job (status, created_at DESC);

CREATE TABLE discovered_document (
    id UUID PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL,
    source_url TEXT,
    document_url TEXT NOT NULL,
    title VARCHAR(500) NOT NULL,
    company_symbol VARCHAR(30),
    document_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    normalized_url TEXT NOT NULL,
    first_discovered_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    seen_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT discovered_document_normalized_url_uk UNIQUE (normalized_url),
    CONSTRAINT discovered_document_source_type_check CHECK (
        source_type IN ('NSE', 'BSE', 'SEBI', 'RBI', 'COMPANY_IR', 'TEST_SOURCE')
    ),
    CONSTRAINT discovered_document_type_check CHECK (
        document_type IN (
            'ANNUAL_REPORT', 'QUARTERLY_RESULT', 'INVESTOR_PRESENTATION',
            'CIRCULAR', 'ANNOUNCEMENT', 'UNKNOWN'
        )
    ),
    CONSTRAINT discovered_document_status_check CHECK (
        status IN ('NEW', 'EXISTING', 'INGESTED', 'IGNORED', 'FAILED')
    ),
    CONSTRAINT discovered_document_seen_count_check CHECK (seen_count >= 1)
);

CREATE INDEX discovered_document_last_seen_idx
    ON discovered_document (last_seen_at DESC);
CREATE INDEX discovered_document_filters_idx
    ON discovered_document (status, source_type, document_type);
CREATE INDEX discovered_document_company_idx
    ON discovered_document (company_symbol, last_seen_at DESC);

CREATE TABLE discovery_source_run (
    id UUID PRIMARY KEY,
    discovery_job_id UUID NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_url TEXT,
    status VARCHAR(20) NOT NULL,
    discovered_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT discovery_source_run_job_fk
        FOREIGN KEY (discovery_job_id) REFERENCES discovery_job (id) ON DELETE CASCADE,
    CONSTRAINT discovery_source_run_source_type_check CHECK (
        source_type IN ('NSE', 'BSE', 'SEBI', 'RBI', 'COMPANY_IR', 'TEST_SOURCE')
    ),
    CONSTRAINT discovery_source_run_status_check CHECK (
        status IN ('STARTED', 'COMPLETED', 'FAILED', 'PARTIAL')
    ),
    CONSTRAINT discovery_source_run_count_check CHECK (discovered_count >= 0)
);

CREATE INDEX discovery_source_run_job_idx
    ON discovery_source_run (discovery_job_id, created_at);
