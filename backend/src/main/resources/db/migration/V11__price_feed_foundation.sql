CREATE TABLE market_instrument (
    id UUID PRIMARY KEY,
    symbol VARCHAR(64) NOT NULL,
    isin VARCHAR(20),
    name VARCHAR(255),
    exchange VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT market_instrument_exchange_check
        CHECK (exchange IN ('NSE', 'BSE', 'UNKNOWN')),
    CONSTRAINT market_instrument_symbol_format_check
        CHECK (symbol ~ '^[A-Z0-9.-]+$'),
    CONSTRAINT market_instrument_exchange_symbol_uk UNIQUE (exchange, symbol)
);

CREATE INDEX market_instrument_symbol_idx ON market_instrument (symbol);
CREATE INDEX market_instrument_active_idx ON market_instrument (active);

CREATE TABLE price_feed_job (
    id UUID PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_instruments INTEGER NOT NULL DEFAULT 0,
    updated_instruments INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT price_feed_job_source_check
        CHECK (source IN ('MANUAL', 'MOCK', 'ZERODHA', 'YAHOO', 'FINNHUB', 'NSE')),
    CONSTRAINT price_feed_job_status_check
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT price_feed_job_count_check
        CHECK (requested_instruments >= 0 AND updated_instruments >= 0)
);

CREATE TABLE price_snapshot (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL,
    feed_job_id UUID,
    last_price NUMERIC(24, 8) NOT NULL,
    previous_close NUMERIC(24, 8) NOT NULL,
    source VARCHAR(30) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT price_snapshot_instrument_fk
        FOREIGN KEY (instrument_id) REFERENCES market_instrument (id) ON DELETE CASCADE,
    CONSTRAINT price_snapshot_feed_job_fk
        FOREIGN KEY (feed_job_id) REFERENCES price_feed_job (id) ON DELETE SET NULL,
    CONSTRAINT price_snapshot_source_check
        CHECK (source IN ('MANUAL', 'MOCK', 'ZERODHA', 'YAHOO', 'FINNHUB', 'NSE')),
    CONSTRAINT price_snapshot_values_check
        CHECK (last_price >= 0 AND previous_close >= 0)
);

CREATE INDEX price_snapshot_instrument_captured_idx
    ON price_snapshot (instrument_id, captured_at DESC);
CREATE INDEX price_snapshot_captured_idx ON price_snapshot (captured_at DESC);
