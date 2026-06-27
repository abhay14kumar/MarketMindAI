CREATE TABLE source_intelligence_profile (
    source_id UUID PRIMARY KEY,
    connector_type VARCHAR(40) NOT NULL,
    trust_tier VARCHAR(20) NOT NULL,
    trust_score INTEGER NOT NULL DEFAULT 50,
    freshness_score INTEGER NOT NULL DEFAULT 0,
    supported_formats JSONB NOT NULL DEFAULT '[]'::JSONB,
    supported_document_types JSONB NOT NULL DEFAULT '[]'::JSONB,
    last_crawl_at TIMESTAMPTZ,
    next_crawl_at TIMESTAMPTZ,
    scheduler_state VARCHAR(40) NOT NULL DEFAULT 'NOT_CONFIGURED',
    total_crawls BIGINT NOT NULL DEFAULT 0,
    successful_crawls BIGINT NOT NULL DEFAULT 0,
    failed_crawls BIGINT NOT NULL DEFAULT 0,
    documents_discovered BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_intelligence_source_fk
        FOREIGN KEY (source_id) REFERENCES source_registry (id) ON DELETE CASCADE,
    CONSTRAINT source_intelligence_trust_score_check CHECK (trust_score BETWEEN 0 AND 100),
    CONSTRAINT source_intelligence_freshness_score_check CHECK (freshness_score BETWEEN 0 AND 100),
    CONSTRAINT source_intelligence_formats_array_check CHECK (JSONB_TYPEOF(supported_formats) = 'array'),
    CONSTRAINT source_intelligence_document_types_array_check
        CHECK (JSONB_TYPEOF(supported_document_types) = 'array')
);

CREATE TABLE source_activity (
    id UUID PRIMARY KEY,
    source_id UUID,
    activity_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(250) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    related_entity_type VARCHAR(80),
    related_entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_activity_source_fk
        FOREIGN KEY (source_id) REFERENCES source_registry (id) ON DELETE SET NULL
);

CREATE INDEX source_activity_occurred_idx ON source_activity (occurred_at DESC);
CREATE INDEX source_activity_source_occurred_idx
    ON source_activity (source_id, occurred_at DESC);

INSERT INTO source_intelligence_profile (
    source_id, connector_type, trust_tier, trust_score, freshness_score,
    supported_formats, supported_document_types, scheduler_state
)
SELECT
    id,
    CASE code
        WHEN 'NSE' THEN 'NSE'
        WHEN 'BSE' THEN 'BSE'
        WHEN 'SEBI' THEN 'SEBI'
        WHEN 'RBI' THEN 'RBI'
        WHEN 'TEST_SOURCE' THEN 'TEST_STATIC'
        ELSE CASE
            WHEN source_type = 'COMPANY_INVESTOR_RELATIONS' THEN 'COMPANY_IR'
            ELSE 'GENERIC_REST'
        END
    END,
    CASE
        WHEN code IN ('NSE', 'BSE', 'SEBI', 'RBI') THEN 'OFFICIAL'
        WHEN source_type = 'TEST_SOURCE' THEN 'TEST'
        ELSE 'THIRD_PARTY'
    END,
    LEAST(100, GREATEST(0, ROUND(reliability_score * 100)::INTEGER)),
    0,
    CASE
        WHEN code IN ('NSE', 'BSE', 'SEBI', 'RBI')
            OR source_type = 'COMPANY_INVESTOR_RELATIONS'
            THEN '["HTML","PDF"]'::JSONB
        WHEN source_type = 'TEST_SOURCE' THEN '["PDF"]'::JSONB
        ELSE '["REST","JSON","XML"]'::JSONB
    END,
    '["ANNUAL_REPORT","QUARTERLY_RESULT","INVESTOR_PRESENTATION","CIRCULAR","ANNOUNCEMENT","UNKNOWN"]'::JSONB,
    CASE WHEN code = 'NSE' THEN 'PARTIAL' ELSE 'NOT_CONFIGURED' END
FROM source_registry
ON CONFLICT (source_id) DO NOTHING;
