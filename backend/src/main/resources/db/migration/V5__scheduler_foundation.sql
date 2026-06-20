CREATE TABLE scheduler_job (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    scheduler_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(120) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}'::JSONB,
    next_run_at TIMESTAMPTZ,
    last_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT scheduler_job_type_check CHECK (
        scheduler_type IN (
            'NSE_FILINGS',
            'BSE_FILINGS',
            'SEBI_FILINGS',
            'COMPANY_FILINGS',
            'CUSTOM'
        )
    ),
    CONSTRAINT scheduler_job_status_check CHECK (
        status IN ('ACTIVE', 'PAUSED', 'DISABLED')
    ),
    CONSTRAINT scheduler_job_cron_not_blank_check CHECK (
        LENGTH(BTRIM(cron_expression)) > 0
    ),
    CONSTRAINT scheduler_job_timezone_not_blank_check CHECK (
        LENGTH(BTRIM(timezone)) > 0
    ),
    CONSTRAINT scheduler_job_configuration_object_check CHECK (
        JSONB_TYPEOF(configuration) = 'object'
    )
);

CREATE UNIQUE INDEX scheduler_job_name_lower_uidx
    ON scheduler_job (LOWER(name));
CREATE INDEX scheduler_job_status_type_idx
    ON scheduler_job (status, scheduler_type);
CREATE INDEX scheduler_job_next_run_idx
    ON scheduler_job (next_run_at)
    WHERE status = 'ACTIVE';

CREATE TABLE scheduler_run (
    id UUID PRIMARY KEY,
    scheduler_job_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    queued_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    processed_items BIGINT NOT NULL DEFAULT 0,
    correlation_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT scheduler_run_job_fk
        FOREIGN KEY (scheduler_job_id) REFERENCES scheduler_job (id) ON DELETE RESTRICT,
    CONSTRAINT scheduler_run_status_check CHECK (
        status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT scheduler_run_trigger_type_check CHECK (
        trigger_type IN ('SCHEDULED', 'MANUAL', 'RETRY')
    ),
    CONSTRAINT scheduler_run_processed_items_check CHECK (processed_items >= 0),
    CONSTRAINT scheduler_run_correlation_uk UNIQUE (correlation_id),
    CONSTRAINT scheduler_run_started_check CHECK (
        started_at IS NULL OR started_at >= queued_at
    ),
    CONSTRAINT scheduler_run_completed_check CHECK (
        completed_at IS NULL
        OR (
            started_at IS NOT NULL
            AND completed_at >= started_at
            AND status IN ('COMPLETED', 'FAILED', 'CANCELLED')
        )
    )
);

CREATE INDEX scheduler_run_job_queued_idx
    ON scheduler_run (scheduler_job_id, queued_at DESC);
CREATE INDEX scheduler_run_status_queued_idx
    ON scheduler_run (status, queued_at);

CREATE TABLE scheduler_error (
    id UUID PRIMARY KEY,
    scheduler_run_id UUID NOT NULL,
    error_code VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    details TEXT,
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT scheduler_error_run_fk
        FOREIGN KEY (scheduler_run_id) REFERENCES scheduler_run (id) ON DELETE CASCADE,
    CONSTRAINT scheduler_error_code_not_blank_check CHECK (
        LENGTH(BTRIM(error_code)) > 0
    ),
    CONSTRAINT scheduler_error_message_not_blank_check CHECK (
        LENGTH(BTRIM(message)) > 0
    )
);

CREATE INDEX scheduler_error_run_occurred_idx
    ON scheduler_error (scheduler_run_id, occurred_at DESC);
CREATE INDEX scheduler_error_retryable_idx
    ON scheduler_error (retryable)
    WHERE retryable = TRUE;
