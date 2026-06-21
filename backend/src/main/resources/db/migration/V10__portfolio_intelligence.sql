CREATE TABLE portfolio (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    broker_type VARCHAR(30) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT portfolio_broker_type_check CHECK (broker_type IN ('ZERODHA')),
    CONSTRAINT portfolio_broker_name_uk UNIQUE (broker_type, name)
);

CREATE TABLE portfolio_import_job (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    broker_type VARCHAR(30) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    imported_rows INTEGER NOT NULL DEFAULT 0,
    rejected_rows INTEGER NOT NULL DEFAULT 0,
    row_errors JSONB NOT NULL DEFAULT '[]'::JSONB,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT portfolio_import_job_portfolio_fk
        FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE,
    CONSTRAINT portfolio_import_job_broker_check CHECK (broker_type IN ('ZERODHA')),
    CONSTRAINT portfolio_import_job_status_check
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT portfolio_import_job_counts_check CHECK (
        total_rows >= 0
        AND imported_rows >= 0
        AND rejected_rows >= 0
        AND imported_rows + rejected_rows <= total_rows
    ),
    CONSTRAINT portfolio_import_job_errors_array_check
        CHECK (JSONB_TYPEOF(row_errors) = 'array')
);

CREATE INDEX portfolio_import_job_portfolio_started_idx
    ON portfolio_import_job (portfolio_id, started_at DESC);

CREATE TABLE portfolio_holding (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    import_job_id UUID NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    isin VARCHAR(20),
    company_name VARCHAR(255),
    sector VARCHAR(120),
    instrument_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(24, 8) NOT NULL,
    average_cost NUMERIC(24, 8) NOT NULL,
    last_price NUMERIC(24, 8),
    previous_close NUMERIC(24, 8),
    invested_value NUMERIC(24, 8) NOT NULL,
    present_value NUMERIC(24, 8) NOT NULL,
    unrealized_pnl NUMERIC(24, 8) NOT NULL,
    unrealized_pnl_percentage NUMERIC(12, 4),
    as_of TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT portfolio_holding_portfolio_fk
        FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE,
    CONSTRAINT portfolio_holding_import_job_fk
        FOREIGN KEY (import_job_id) REFERENCES portfolio_import_job (id) ON DELETE RESTRICT,
    CONSTRAINT portfolio_holding_instrument_check CHECK (
        instrument_type IN ('EQUITY', 'ETF', 'MUTUAL_FUND', 'UNKNOWN')
    ),
    CONSTRAINT portfolio_holding_quantity_check CHECK (quantity >= 0),
    CONSTRAINT portfolio_holding_average_cost_check CHECK (average_cost >= 0),
    CONSTRAINT portfolio_holding_invested_value_check CHECK (invested_value >= 0),
    CONSTRAINT portfolio_holding_present_value_check CHECK (present_value >= 0),
    CONSTRAINT portfolio_holding_portfolio_symbol_uk UNIQUE (portfolio_id, symbol)
);

CREATE INDEX portfolio_holding_portfolio_idx ON portfolio_holding (portfolio_id);
CREATE INDEX portfolio_holding_sector_idx ON portfolio_holding (portfolio_id, sector);
CREATE INDEX portfolio_holding_instrument_idx
    ON portfolio_holding (portfolio_id, instrument_type);

CREATE TABLE portfolio_snapshot (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    import_job_id UUID NOT NULL,
    total_invested_value NUMERIC(24, 8) NOT NULL,
    total_present_value NUMERIC(24, 8) NOT NULL,
    total_unrealized_pnl NUMERIC(24, 8) NOT NULL,
    total_unrealized_pnl_percentage NUMERIC(12, 4),
    total_holdings INTEGER NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT portfolio_snapshot_portfolio_fk
        FOREIGN KEY (portfolio_id) REFERENCES portfolio (id) ON DELETE CASCADE,
    CONSTRAINT portfolio_snapshot_import_job_fk
        FOREIGN KEY (import_job_id) REFERENCES portfolio_import_job (id) ON DELETE RESTRICT,
    CONSTRAINT portfolio_snapshot_values_check CHECK (
        total_invested_value >= 0
        AND total_present_value >= 0
        AND total_holdings >= 0
    )
);

CREATE INDEX portfolio_snapshot_portfolio_captured_idx
    ON portfolio_snapshot (portfolio_id, captured_at DESC);
