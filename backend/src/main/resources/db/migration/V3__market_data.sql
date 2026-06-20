CREATE TABLE exchange (
    id UUID PRIMARY KEY,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    country CHAR(2) NOT NULL,
    currency CHAR(3) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT exchange_code_uk UNIQUE (code),
    CONSTRAINT exchange_code_format_check CHECK (code ~ '^[A-Z0-9.-]+$'),
    CONSTRAINT exchange_country_format_check CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT exchange_currency_format_check CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX exchange_active_idx ON exchange (active);

CREATE TABLE market_index (
    id UUID PRIMARY KEY,
    exchange_id UUID NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(150) NOT NULL,
    currency CHAR(3) NOT NULL,
    last_value NUMERIC(24, 8),
    change_value NUMERIC(24, 8),
    change_percent NUMERIC(12, 6),
    as_of TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT market_index_exchange_fk
        FOREIGN KEY (exchange_id) REFERENCES exchange (id) ON DELETE RESTRICT,
    CONSTRAINT market_index_exchange_symbol_uk UNIQUE (exchange_id, symbol),
    CONSTRAINT market_index_symbol_format_check CHECK (symbol ~ '^[A-Z0-9.-]+$'),
    CONSTRAINT market_index_currency_format_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT market_index_last_value_check CHECK (last_value IS NULL OR last_value >= 0),
    CONSTRAINT market_index_observation_check CHECK (
        (last_value IS NULL AND as_of IS NULL)
        OR (last_value IS NOT NULL AND as_of IS NOT NULL)
    )
);

CREATE INDEX market_index_active_idx ON market_index (active);
CREATE INDEX market_index_as_of_idx ON market_index (as_of DESC);

CREATE TABLE stock_price_daily (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    exchange_id UUID NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    trading_date DATE NOT NULL,
    open_price NUMERIC(24, 8) NOT NULL,
    high_price NUMERIC(24, 8) NOT NULL,
    low_price NUMERIC(24, 8) NOT NULL,
    close_price NUMERIC(24, 8) NOT NULL,
    adjusted_close NUMERIC(24, 8),
    volume BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    source VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stock_price_daily_company_fk
        FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT stock_price_daily_exchange_fk
        FOREIGN KEY (exchange_id) REFERENCES exchange (id) ON DELETE RESTRICT,
    CONSTRAINT stock_price_daily_observation_uk
        UNIQUE (exchange_id, symbol, trading_date, source),
    CONSTRAINT stock_price_daily_symbol_format_check CHECK (symbol ~ '^[A-Z0-9.-]+$'),
    CONSTRAINT stock_price_daily_prices_check CHECK (
        open_price >= 0
        AND high_price >= 0
        AND low_price >= 0
        AND close_price >= 0
        AND (adjusted_close IS NULL OR adjusted_close >= 0)
        AND high_price >= low_price
    ),
    CONSTRAINT stock_price_daily_volume_check CHECK (volume >= 0),
    CONSTRAINT stock_price_daily_currency_format_check CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX stock_price_daily_symbol_date_idx
    ON stock_price_daily (exchange_id, symbol, trading_date DESC);
CREATE INDEX stock_price_daily_company_date_idx
    ON stock_price_daily (company_id, trading_date DESC);
