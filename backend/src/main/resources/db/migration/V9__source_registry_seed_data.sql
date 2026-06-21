ALTER TABLE source_registry
    ADD COLUMN organization VARCHAR(150),
    ADD COLUMN robots_url TEXT,
    ADD COLUMN priority INTEGER NOT NULL DEFAULT 50,
    ADD COLUMN reliability_score NUMERIC(5, 4) NOT NULL DEFAULT 0.5000;

UPDATE source_registry
SET organization = name
WHERE organization IS NULL;

ALTER TABLE source_registry
    ALTER COLUMN organization SET NOT NULL,
    DROP CONSTRAINT source_registry_type_check;

ALTER TABLE source_registry
    ADD CONSTRAINT source_registry_type_check CHECK (
        source_type IN (
            'EXCHANGE',
            'REGULATOR',
            'CENTRAL_BANK',
            'MUTUAL_FUND_ASSOCIATION',
            'MARKET_DATA_PROVIDER',
            'COMPANY_INVESTOR_RELATIONS',
            'TEST_SOURCE'
        )
    ),
    ADD CONSTRAINT source_registry_robots_url_check CHECK (
        robots_url IS NULL
        OR robots_url ~ '^https?://[^[:space:]]+$'
    ),
    ADD CONSTRAINT source_registry_priority_check CHECK (
        priority BETWEEN 1 AND 100
    ),
    ADD CONSTRAINT source_registry_reliability_score_check CHECK (
        reliability_score BETWEEN 0.0000 AND 1.0000
    );

ALTER TABLE source_capability
    DROP CONSTRAINT source_capability_type_check;

ALTER TABLE source_capability
    ADD CONSTRAINT source_capability_type_check CHECK (
        capability_type IN (
            'COMPANY_MASTER',
            'MARKET_PRICES',
            'MARKET_INDEXES',
            'COMPANY_FILINGS',
            'REGULATORY_FILINGS',
            'MUTUAL_FUND_DATA',
            'CORPORATE_ACTIONS',
            'FINANCIAL_STATEMENTS',
            'INVESTOR_RELATIONS_DOCUMENTS',
            'PDF_DOWNLOAD',
            'ROBOTS_TXT',
            'HTTP_REACHABILITY',
            'MARKET_DATA',
            'CORPORATE_FILINGS',
            'FINANCIAL_RESULTS',
            'ANNUAL_REPORTS',
            'DIVIDENDS',
            'SHAREHOLDING',
            'MACRO_DATA'
        )
    );

INSERT INTO source_registry (
    id,
    code,
    name,
    organization,
    description,
    source_type,
    status,
    authentication_type,
    refresh_frequency,
    base_url,
    robots_url,
    documentation_url,
    sample_pdf_url,
    enabled,
    priority,
    reliability_score
) VALUES
    (
        '71000000-0000-0000-0000-000000000001',
        'NSE',
        'National Stock Exchange of India',
        'National Stock Exchange of India Limited',
        'Official exchange source for Indian market data, corporate filings, and results.',
        'EXCHANGE',
        'ACTIVE',
        'SESSION',
        'MINUTELY',
        'https://www.nseindia.com',
        'https://www.nseindia.com/robots.txt',
        'https://www.nseindia.com',
        NULL,
        TRUE,
        10,
        0.9800
    ),
    (
        '71000000-0000-0000-0000-000000000002',
        'BSE',
        'BSE Limited',
        'BSE Limited',
        'Official exchange source for BSE market data, corporate filings, and results.',
        'EXCHANGE',
        'ACTIVE',
        'NONE',
        'MINUTELY',
        'https://www.bseindia.com',
        'https://www.bseindia.com/robots.txt',
        'https://www.bseindia.com',
        NULL,
        TRUE,
        10,
        0.9800
    ),
    (
        '71000000-0000-0000-0000-000000000003',
        'SEBI',
        'Securities and Exchange Board of India',
        'Securities and Exchange Board of India',
        'Official Indian securities regulator and regulatory publication source.',
        'REGULATOR',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.sebi.gov.in',
        'https://www.sebi.gov.in/robots.txt',
        'https://www.sebi.gov.in',
        NULL,
        TRUE,
        10,
        0.9900
    ),
    (
        '71000000-0000-0000-0000-000000000004',
        'RBI',
        'Reserve Bank of India',
        'Reserve Bank of India',
        'Official central bank source for monetary, banking, and macroeconomic data.',
        'CENTRAL_BANK',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.rbi.org.in',
        'https://www.rbi.org.in/robots.txt',
        'https://www.rbi.org.in',
        NULL,
        TRUE,
        10,
        0.9900
    ),
    (
        '71000000-0000-0000-0000-000000000005',
        'AMFI',
        'Association of Mutual Funds in India',
        'Association of Mutual Funds in India',
        'Industry source for Indian mutual fund reference and market data.',
        'MUTUAL_FUND_ASSOCIATION',
        'ACTIVE',
        'NONE',
        'DAILY',
        'https://www.amfiindia.com',
        'https://www.amfiindia.com/robots.txt',
        'https://www.amfiindia.com',
        NULL,
        TRUE,
        20,
        0.9500
    ),
    (
        '71000000-0000-0000-0000-000000000006',
        'YAHOO_FINANCE',
        'Yahoo Finance',
        'Yahoo',
        'Third-party financial news and market data source.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'NONE',
        'MINUTELY',
        'https://finance.yahoo.com',
        'https://finance.yahoo.com/robots.txt',
        'https://finance.yahoo.com',
        NULL,
        TRUE,
        40,
        0.8000
    ),
    (
        '71000000-0000-0000-0000-000000000007',
        'FINNHUB',
        'Finnhub',
        'Finnhub',
        'Third-party market data provider; credentials must be managed outside the registry.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'API_KEY',
        'REAL_TIME',
        'https://finnhub.io',
        'https://finnhub.io/robots.txt',
        'https://finnhub.io/docs/api',
        NULL,
        TRUE,
        30,
        0.8800
    ),
    (
        '71000000-0000-0000-0000-000000000008',
        'ALPHAVANTAGE',
        'AlphaVantage',
        'Alpha Vantage',
        'Third-party market data provider; credentials must be managed outside the registry.',
        'MARKET_DATA_PROVIDER',
        'ACTIVE',
        'API_KEY',
        'MINUTELY',
        'https://www.alphavantage.co',
        'https://www.alphavantage.co/robots.txt',
        'https://www.alphavantage.co/documentation',
        NULL,
        TRUE,
        30,
        0.8500
    ),
    (
        '71000000-0000-0000-0000-000000000009',
        'W3C_TEST',
        'W3C Test Source',
        'World Wide Web Consortium',
        'Stable public test source for HTTP, robots.txt, and PDF capability validation.',
        'TEST_SOURCE',
        'ACTIVE',
        'NONE',
        'ON_DEMAND',
        'https://www.w3.org',
        'https://www.w3.org/robots.txt',
        'https://www.w3.org',
        'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
        TRUE,
        100,
        0.9900
    )
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    organization = EXCLUDED.organization,
    description = EXCLUDED.description,
    source_type = EXCLUDED.source_type,
    status = EXCLUDED.status,
    authentication_type = EXCLUDED.authentication_type,
    refresh_frequency = EXCLUDED.refresh_frequency,
    base_url = EXCLUDED.base_url,
    robots_url = EXCLUDED.robots_url,
    documentation_url = EXCLUDED.documentation_url,
    sample_pdf_url = EXCLUDED.sample_pdf_url,
    enabled = EXCLUDED.enabled,
    priority = EXCLUDED.priority,
    reliability_score = EXCLUDED.reliability_score,
    updated_at = CURRENT_TIMESTAMP;

WITH capability_seed(source_code, capability_type) AS (
    VALUES
        ('NSE', 'HTTP_REACHABILITY'),
        ('NSE', 'ROBOTS_TXT'),
        ('NSE', 'MARKET_DATA'),
        ('NSE', 'CORPORATE_FILINGS'),
        ('NSE', 'FINANCIAL_RESULTS'),
        ('NSE', 'ANNUAL_REPORTS'),
        ('NSE', 'DIVIDENDS'),
        ('NSE', 'SHAREHOLDING'),
        ('BSE', 'HTTP_REACHABILITY'),
        ('BSE', 'ROBOTS_TXT'),
        ('BSE', 'MARKET_DATA'),
        ('BSE', 'CORPORATE_FILINGS'),
        ('BSE', 'FINANCIAL_RESULTS'),
        ('BSE', 'ANNUAL_REPORTS'),
        ('BSE', 'DIVIDENDS'),
        ('BSE', 'SHAREHOLDING'),
        ('SEBI', 'HTTP_REACHABILITY'),
        ('SEBI', 'ROBOTS_TXT'),
        ('SEBI', 'CORPORATE_FILINGS'),
        ('SEBI', 'ANNUAL_REPORTS'),
        ('RBI', 'HTTP_REACHABILITY'),
        ('RBI', 'ROBOTS_TXT'),
        ('RBI', 'PDF_DOWNLOAD'),
        ('RBI', 'MACRO_DATA'),
        ('AMFI', 'HTTP_REACHABILITY'),
        ('AMFI', 'ROBOTS_TXT'),
        ('AMFI', 'MARKET_DATA'),
        ('YAHOO_FINANCE', 'HTTP_REACHABILITY'),
        ('YAHOO_FINANCE', 'ROBOTS_TXT'),
        ('YAHOO_FINANCE', 'MARKET_DATA'),
        ('YAHOO_FINANCE', 'DIVIDENDS'),
        ('FINNHUB', 'HTTP_REACHABILITY'),
        ('FINNHUB', 'ROBOTS_TXT'),
        ('FINNHUB', 'MARKET_DATA'),
        ('FINNHUB', 'FINANCIAL_RESULTS'),
        ('ALPHAVANTAGE', 'HTTP_REACHABILITY'),
        ('ALPHAVANTAGE', 'ROBOTS_TXT'),
        ('ALPHAVANTAGE', 'MARKET_DATA'),
        ('ALPHAVANTAGE', 'FINANCIAL_RESULTS'),
        ('W3C_TEST', 'HTTP_REACHABILITY'),
        ('W3C_TEST', 'ROBOTS_TXT'),
        ('W3C_TEST', 'PDF_DOWNLOAD')
)
INSERT INTO source_capability (
    id,
    source_id,
    capability_type,
    supported,
    verified_at
)
SELECT
    MD5(source.code || '-' || capability_seed.capability_type)::UUID,
    source.id,
    capability_seed.capability_type,
    TRUE,
    CURRENT_TIMESTAMP
FROM capability_seed
JOIN source_registry source ON source.code = capability_seed.source_code
ON CONFLICT (source_id, capability_type) DO UPDATE SET
    supported = EXCLUDED.supported,
    verified_at = EXCLUDED.verified_at;
