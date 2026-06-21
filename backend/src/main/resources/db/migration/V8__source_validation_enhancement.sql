ALTER TABLE source_registry
    ADD COLUMN sample_pdf_url TEXT;

ALTER TABLE source_registry
    DROP CONSTRAINT source_registry_base_url_check,
    DROP CONSTRAINT source_registry_documentation_url_check;

ALTER TABLE source_registry
    ADD CONSTRAINT source_registry_base_url_check
        CHECK (base_url ~ '^https?://[^[:space:]]+$'),
    ADD CONSTRAINT source_registry_documentation_url_check
        CHECK (
            documentation_url IS NULL
            OR documentation_url ~ '^https?://[^[:space:]]+$'
        ),
    ADD CONSTRAINT source_registry_sample_pdf_url_check
        CHECK (
            sample_pdf_url IS NULL
            OR sample_pdf_url ~ '^https?://[^[:space:]]+$'
        );

ALTER TABLE source_health
    ADD COLUMN last_http_status INTEGER,
    ADD COLUMN last_latency_ms BIGINT,
    ADD COLUMN robots_txt_available BOOLEAN,
    ADD COLUMN robots_txt_status INTEGER,
    ADD COLUMN pdf_capability_status VARCHAR(20),
    ADD COLUMN last_validated_at TIMESTAMPTZ;

ALTER TABLE source_health
    ADD CONSTRAINT source_health_http_status_check
        CHECK (last_http_status IS NULL OR last_http_status BETWEEN 100 AND 599),
    ADD CONSTRAINT source_health_last_latency_check
        CHECK (last_latency_ms IS NULL OR last_latency_ms >= 0),
    ADD CONSTRAINT source_health_robots_status_check
        CHECK (robots_txt_status IS NULL OR robots_txt_status BETWEEN 100 AND 599),
    ADD CONSTRAINT source_health_pdf_capability_check
        CHECK (
            pdf_capability_status IS NULL
            OR pdf_capability_status IN ('SUPPORTED', 'UNSUPPORTED', 'UNKNOWN')
        );

ALTER TABLE source_validation_history
    ADD COLUMN http_status INTEGER,
    ADD COLUMN robots_txt_available BOOLEAN,
    ADD COLUMN robots_txt_status INTEGER,
    ADD COLUMN pdf_capability_status VARCHAR(20);

ALTER TABLE source_validation_history
    ADD CONSTRAINT source_validation_http_status_check
        CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599),
    ADD CONSTRAINT source_validation_robots_status_check
        CHECK (robots_txt_status IS NULL OR robots_txt_status BETWEEN 100 AND 599),
    ADD CONSTRAINT source_validation_pdf_capability_check
        CHECK (
            pdf_capability_status IS NULL
            OR pdf_capability_status IN ('SUPPORTED', 'UNSUPPORTED', 'UNKNOWN')
        );

CREATE INDEX source_health_last_validated_idx
    ON source_health (source_id, last_validated_at DESC);
