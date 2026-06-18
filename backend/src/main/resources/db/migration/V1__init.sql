CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE UNIQUE INDEX users_email_lower_uidx ON users (LOWER(email));

CREATE TABLE stocks (
    id UUID PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    exchange VARCHAR(32) NOT NULL,
    isin VARCHAR(12),
    company_name VARCHAR(255) NOT NULL,
    sector VARCHAR(120),
    industry VARCHAR(120),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stocks_exchange_symbol_uk UNIQUE (exchange, symbol),
    CONSTRAINT stocks_isin_uk UNIQUE (isin)
);

CREATE INDEX stocks_company_name_idx ON stocks (company_name);

CREATE TABLE holdings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    account_label VARCHAR(100) NOT NULL DEFAULT 'DEFAULT',
    quantity NUMERIC(24, 8) NOT NULL,
    average_cost NUMERIC(24, 8) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    as_of TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT holdings_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT holdings_stock_fk FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE RESTRICT,
    CONSTRAINT holdings_user_stock_account_uk UNIQUE (user_id, stock_id, account_label),
    CONSTRAINT holdings_quantity_check CHECK (quantity >= 0),
    CONSTRAINT holdings_average_cost_check CHECK (average_cost >= 0)
);

CREATE INDEX holdings_user_id_idx ON holdings (user_id);
CREATE INDEX holdings_stock_id_idx ON holdings (stock_id);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    trade_date DATE NOT NULL,
    quantity NUMERIC(24, 8) NOT NULL,
    price NUMERIC(24, 8),
    fees NUMERIC(24, 8) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactions_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT transactions_stock_fk FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE RESTRICT,
    CONSTRAINT transactions_type_check CHECK (
        transaction_type IN ('BUY', 'SELL', 'DIVIDEND', 'SPLIT', 'BONUS', 'ADJUSTMENT')
    ),
    CONSTRAINT transactions_quantity_check CHECK (quantity > 0),
    CONSTRAINT transactions_price_check CHECK (price IS NULL OR price >= 0),
    CONSTRAINT transactions_fees_check CHECK (fees >= 0)
);

CREATE INDEX transactions_user_trade_date_idx ON transactions (user_id, trade_date DESC);
CREATE INDEX transactions_stock_id_idx ON transactions (stock_id);

CREATE TABLE watchlists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT watchlists_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT watchlists_user_name_uk UNIQUE (user_id, name)
);

CREATE INDEX watchlists_user_id_idx ON watchlists (user_id);

CREATE TABLE watchlist_stocks (
    watchlist_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    notes VARCHAR(500),
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (watchlist_id, stock_id),
    CONSTRAINT watchlist_stocks_watchlist_fk
        FOREIGN KEY (watchlist_id) REFERENCES watchlists (id) ON DELETE CASCADE,
    CONSTRAINT watchlist_stocks_stock_fk
        FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
);

CREATE INDEX watchlist_stocks_stock_id_idx ON watchlist_stocks (stock_id);

CREATE TABLE price_snapshots (
    id UUID PRIMARY KEY,
    stock_id UUID NOT NULL,
    price NUMERIC(24, 8) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    source VARCHAR(100) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT price_snapshots_stock_fk FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE,
    CONSTRAINT price_snapshots_price_check CHECK (price >= 0),
    CONSTRAINT price_snapshots_stock_source_as_of_uk UNIQUE (stock_id, source, as_of)
);

CREATE INDEX price_snapshots_stock_as_of_idx ON price_snapshots (stock_id, as_of DESC);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    stock_id UUID,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    source VARCHAR(150) NOT NULL,
    source_url TEXT,
    storage_reference TEXT,
    checksum_sha256 CHAR(64) NOT NULL,
    publication_date DATE,
    reporting_period VARCHAR(50),
    ingestion_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT documents_stock_fk FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE SET NULL,
    CONSTRAINT documents_checksum_uk UNIQUE (checksum_sha256),
    CONSTRAINT documents_type_check CHECK (
        document_type IN (
            'ANNUAL_REPORT',
            'QUARTERLY_RESULT',
            'EXCHANGE_FILING',
            'CONCALL_TRANSCRIPT',
            'INVESTOR_PRESENTATION',
            'OTHER'
        )
    ),
    CONSTRAINT documents_ingestion_status_check CHECK (
        ingestion_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX documents_stock_publication_date_idx ON documents (stock_id, publication_date DESC);
CREATE INDEX documents_ingestion_status_idx ON documents (ingestion_status);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    stock_id UUID,
    alert_type VARCHAR(40) NOT NULL,
    condition_operator VARCHAR(20),
    threshold_value NUMERIC(24, 8),
    message VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT alerts_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT alerts_stock_fk FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE,
    CONSTRAINT alerts_type_check CHECK (
        alert_type IN ('PRICE', 'DOCUMENT', 'PORTFOLIO', 'RISK', 'CUSTOM')
    ),
    CONSTRAINT alerts_condition_operator_check CHECK (
        condition_operator IS NULL
        OR condition_operator IN ('GT', 'GTE', 'LT', 'LTE', 'EQ')
    ),
    CONSTRAINT alerts_threshold_check CHECK (threshold_value IS NULL OR threshold_value >= 0)
);

CREATE INDEX alerts_user_enabled_idx ON alerts (user_id, enabled);
CREATE INDEX alerts_stock_id_idx ON alerts (stock_id);
