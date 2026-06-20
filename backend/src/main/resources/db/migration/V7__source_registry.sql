CREATE TABLE source_registry (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    source_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    authentication_type VARCHAR(20) NOT NULL,
    refresh_frequency VARCHAR(20) NOT NULL,
    base_url TEXT NOT NULL,
    documentation_url TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_registry_code_uk UNIQUE (code),
    CONSTRAINT source_registry_code_format_check
        CHECK (code ~ '^[A-Z0-9][A-Z0-9._-]{0,63}$'),
    CONSTRAINT source_registry_type_check CHECK (
        source_type IN (
            'EXCHANGE',
            'REGULATOR',
            'CENTRAL_BANK',
            'MUTUAL_FUND_ASSOCIATION',
            'MARKET_DATA_PROVIDER',
            'COMPANY_INVESTOR_RELATIONS'
        )
    ),
    CONSTRAINT source_registry_status_check CHECK (
        status IN ('ACTIVE', 'DEGRADED', 'INACTIVE', 'DISABLED')
    ),
    CONSTRAINT source_registry_authentication_check CHECK (
        authentication_type IN ('NONE', 'API_KEY', 'OAUTH2', 'BASIC', 'SESSION')
    ),
    CONSTRAINT source_registry_refresh_check CHECK (
        refresh_frequency IN (
            'REAL_TIME',
            'MINUTELY',
            'HOURLY',
            'DAILY',
            'WEEKLY',
            'ON_DEMAND'
        )
    ),
    CONSTRAINT source_registry_base_url_check
        CHECK (base_url ~ '^https://[^[:space:]]+$'),
    CONSTRAINT source_registry_documentation_url_check
        CHECK (
            documentation_url IS NULL
            OR documentation_url ~ '^https://[^[:space:]]+$'
        )
);

CREATE UNIQUE INDEX source_registry_code_upper_uidx
    ON source_registry (UPPER(code));
CREATE INDEX source_registry_status_type_idx
    ON source_registry (status, source_type);
CREATE INDEX source_registry_enabled_idx ON source_registry (enabled);

CREATE TABLE source_health (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    available BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    message VARCHAR(1000),
    checked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_health_source_fk
        FOREIGN KEY (source_id) REFERENCES source_registry (id) ON DELETE CASCADE,
    CONSTRAINT source_health_status_check CHECK (
        status IN ('ACTIVE', 'DEGRADED', 'INACTIVE', 'DISABLED')
    ),
    CONSTRAINT source_health_latency_check CHECK (latency_ms >= 0)
);

CREATE INDEX source_health_source_checked_idx
    ON source_health (source_id, checked_at DESC);

CREATE TABLE source_capability (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL,
    capability_type VARCHAR(50) NOT NULL,
    supported BOOLEAN NOT NULL DEFAULT TRUE,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_capability_source_fk
        FOREIGN KEY (source_id) REFERENCES source_registry (id) ON DELETE CASCADE,
    CONSTRAINT source_capability_source_type_uk
        UNIQUE (source_id, capability_type),
    CONSTRAINT source_capability_type_check CHECK (
        capability_type IN (
            'COMPANY_MASTER',
            'MARKET_PRICES',
            'MARKET_INDEXES',
            'COMPANY_FILINGS',
            'REGULATORY_FILINGS',
            'MUTUAL_FUND_DATA',
            'CORPORATE_ACTIONS',
            'FINANCIAL_STATEMENTS',
            'INVESTOR_RELATIONS_DOCUMENTS'
        )
    )
);

CREATE INDEX source_capability_type_supported_idx
    ON source_capability (capability_type, supported);

CREATE TABLE source_validation_history (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL,
    validation_status VARCHAR(20) NOT NULL,
    available BOOLEAN NOT NULL,
    latency_ms BIGINT NOT NULL,
    message VARCHAR(1000),
    supported_capabilities JSONB NOT NULL DEFAULT '[]'::JSONB,
    validated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT source_validation_source_fk
        FOREIGN KEY (source_id) REFERENCES source_registry (id) ON DELETE CASCADE,
    CONSTRAINT source_validation_status_check CHECK (
        validation_status IN ('SUCCESS', 'WARNING', 'FAILED')
    ),
    CONSTRAINT source_validation_latency_check CHECK (latency_ms >= 0),
    CONSTRAINT source_validation_capabilities_array_check CHECK (
        JSONB_TYPEOF(supported_capabilities) = 'array'
    )
);

CREATE INDEX source_validation_source_validated_idx
    ON source_validation_history (source_id, validated_at DESC);

INSERT INTO source_registry (
    id,
    code,
    name,
    description,
    source_type,
    status,
    authentication_type,
    refresh_frequency,
    base_url,
    documentation_url,
    enabled
) VALUES
    (
        '71000000-0000-0000-0000-000000000001',
        'NSE',
        'National Stock Exchange of India',
        'Official exchange source for Indian market data and company filings.',
        'EXCHANGE',
        'ACTIVE',
        'SESSION',
        'MINUTELY',
        'https://www.nseindia.com',
        'https://www.nseindia.com',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000002',
        'BSE',
        'BSE Limited',
        'Official exchange source for BSE market data and company filings.',
        'EXCHANGE',
        'ACTIVE',
        'NONE',
        'MINUTELY',
        'https://www.bseindia.com',
        'https://www.bseindia.com',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000003',
        'SEBI',
        'Securities and Exchange Board of India',
        'Official Indian securities regulator.',
        'REGULATOR',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.sebi.gov.in',
        'https://www.sebi.gov.in',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000004',
        'RBI',
        'Reserve Bank of India',
        'Official central bank and financial regulation source.',
        'CENTRAL_BANK',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.rbi.org.in',
        'https://www.rbi.org.in',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000005',
        'AMFI',
        'Association of Mutual Funds in India',
        'Official Indian mutual fund association data source.',
        'MUTUAL_FUND_ASSOCIATION',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.amfiindia.com',
        'https://www.amfiindia.com',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000006',
        'YAHOO_FINANCE',
        'Yahoo Finance',
        'Third-party market data provider metadata entry.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'NONE',
        'MINUTELY',
        'https://finance.yahoo.com',
        'https://finance.yahoo.com',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000007',
        'FINNHUB',
        'Finnhub',
        'Third-party market data provider requiring externally managed credentials.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'API_KEY',
        'REAL_TIME',
        'https://finnhub.io',
        'https://finnhub.io/docs/api',
        TRUE
    ),
    (
        '71000000-0000-0000-0000-000000000008',
        'ALPHAVANTAGE',
        'AlphaVantage',
        'Third-party market data provider requiring externally managed credentials.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'API_KEY',
        'MINUTELY',
        'https://www.alphavantage.co',
        'https://www.alphavantage.co/documentation',
        TRUE
    );

INSERT INTO source_capability (
    id,
    source_id,
    capability_type,
    supported,
    verified_at
) VALUES
    (MD5('NSE-COMPANY_MASTER')::UUID, '71000000-0000-0000-0000-000000000001', 'COMPANY_MASTER', TRUE, CURRENT_TIMESTAMP),
    (MD5('NSE-MARKET_PRICES')::UUID, '71000000-0000-0000-0000-000000000001', 'MARKET_PRICES', TRUE, CURRENT_TIMESTAMP),
    (MD5('NSE-MARKET_INDEXES')::UUID, '71000000-0000-0000-0000-000000000001', 'MARKET_INDEXES', TRUE, CURRENT_TIMESTAMP),
    (MD5('NSE-COMPANY_FILINGS')::UUID, '71000000-0000-0000-0000-000000000001', 'COMPANY_FILINGS', TRUE, CURRENT_TIMESTAMP),
    (MD5('BSE-MARKET_PRICES')::UUID, '71000000-0000-0000-0000-000000000002', 'MARKET_PRICES', TRUE, CURRENT_TIMESTAMP),
    (MD5('BSE-COMPANY_FILINGS')::UUID, '71000000-0000-0000-0000-000000000002', 'COMPANY_FILINGS', TRUE, CURRENT_TIMESTAMP),
    (MD5('SEBI-REGULATORY_FILINGS')::UUID, '71000000-0000-0000-0000-000000000003', 'REGULATORY_FILINGS', TRUE, CURRENT_TIMESTAMP),
    (MD5('RBI-REGULATORY_FILINGS')::UUID, '71000000-0000-0000-0000-000000000004', 'REGULATORY_FILINGS', TRUE, CURRENT_TIMESTAMP),
    (MD5('AMFI-MUTUAL_FUND_DATA')::UUID, '71000000-0000-0000-0000-000000000005', 'MUTUAL_FUND_DATA', TRUE, CURRENT_TIMESTAMP),
    (MD5('YAHOO-MARKET_PRICES')::UUID, '71000000-0000-0000-0000-000000000006', 'MARKET_PRICES', TRUE, CURRENT_TIMESTAMP),
    (MD5('FINNHUB-MARKET_PRICES')::UUID, '71000000-0000-0000-0000-000000000007', 'MARKET_PRICES', TRUE, CURRENT_TIMESTAMP),
    (MD5('ALPHAVANTAGE-MARKET_PRICES')::UUID, '71000000-0000-0000-0000-000000000008', 'MARKET_PRICES', TRUE, CURRENT_TIMESTAMP);

INSERT INTO source_health (
    id,
    source_id,
    status,
    available,
    latency_ms,
    message,
    checked_at
)
SELECT
    MD5(code || '-health')::UUID,
    id,
    'ACTIVE',
    TRUE,
    100,
    'Seeded mock health observation.',
    CURRENT_TIMESTAMP
FROM source_registry;
