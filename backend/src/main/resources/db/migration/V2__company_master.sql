ALTER TABLE stocks RENAME TO companies;

ALTER TABLE companies RENAME CONSTRAINT stocks_pkey TO companies_pkey;
ALTER TABLE companies DROP CONSTRAINT stocks_exchange_symbol_uk;
ALTER TABLE companies DROP CONSTRAINT stocks_isin_uk;

ALTER TABLE companies
    ADD COLUMN nse_symbol VARCHAR(32),
    ADD COLUMN bse_symbol VARCHAR(32),
    ADD COLUMN market_cap_category VARCHAR(30) NOT NULL DEFAULT 'UNCLASSIFIED',
    ADD COLUMN country VARCHAR(2) NOT NULL DEFAULT 'IN',
    ADD COLUMN website VARCHAR(500),
    ADD COLUMN listing_date DATE;

UPDATE companies
SET nse_symbol = CASE
        WHEN UPPER(exchange) = 'NSE' THEN UPPER(symbol)
        WHEN UPPER(exchange) NOT IN ('NSE', 'BSE') THEN UPPER(symbol)
        ELSE NULL
    END,
    bse_symbol = CASE
        WHEN UPPER(exchange) = 'BSE' THEN UPPER(symbol)
        ELSE NULL
    END,
    isin = UPPER(isin),
    currency = UPPER(currency);

ALTER TABLE companies
    DROP COLUMN symbol,
    DROP COLUMN exchange;

ALTER TABLE companies
    ALTER COLUMN isin SET NOT NULL,
    ALTER COLUMN currency TYPE VARCHAR(3),
    ALTER COLUMN market_cap_category DROP DEFAULT,
    ALTER COLUMN country DROP DEFAULT;

ALTER TABLE companies
    ADD CONSTRAINT companies_isin_format_check
        CHECK (isin ~ '^[A-Z]{2}[A-Z0-9]{9}[0-9]$'),
    ADD CONSTRAINT companies_symbol_required_check
        CHECK (nse_symbol IS NOT NULL OR bse_symbol IS NOT NULL),
    ADD CONSTRAINT companies_market_cap_category_check
        CHECK (market_cap_category IN (
            'LARGE_CAP',
            'MID_CAP',
            'SMALL_CAP',
            'MICRO_CAP',
            'UNCLASSIFIED'
        )),
    ADD CONSTRAINT companies_country_format_check
        CHECK (country ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT companies_currency_format_check
        CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT companies_website_format_check
        CHECK (website IS NULL OR website ~ '^https?://[^[:space:]]+$');

CREATE UNIQUE INDEX companies_isin_upper_uidx ON companies (UPPER(isin));
CREATE UNIQUE INDEX companies_nse_symbol_upper_uidx
    ON companies (UPPER(nse_symbol))
    WHERE nse_symbol IS NOT NULL;
CREATE UNIQUE INDEX companies_bse_symbol_upper_uidx
    ON companies (UPPER(bse_symbol))
    WHERE bse_symbol IS NOT NULL;
CREATE INDEX companies_company_name_lower_idx ON companies (LOWER(company_name));
CREATE INDEX companies_sector_industry_idx ON companies (sector, industry);
CREATE INDEX companies_active_idx ON companies (active);

DROP INDEX stocks_company_name_idx;

ALTER TABLE holdings RENAME COLUMN stock_id TO company_id;
ALTER TABLE holdings RENAME CONSTRAINT holdings_stock_fk TO holdings_company_fk;
ALTER TABLE holdings RENAME CONSTRAINT holdings_user_stock_account_uk TO holdings_user_company_account_uk;
ALTER INDEX holdings_stock_id_idx RENAME TO holdings_company_id_idx;

ALTER TABLE transactions RENAME COLUMN stock_id TO company_id;
ALTER TABLE transactions RENAME CONSTRAINT transactions_stock_fk TO transactions_company_fk;
ALTER INDEX transactions_stock_id_idx RENAME TO transactions_company_id_idx;

ALTER TABLE watchlist_stocks RENAME TO watchlist_companies;
ALTER TABLE watchlist_companies RENAME COLUMN stock_id TO company_id;
ALTER TABLE watchlist_companies
    RENAME CONSTRAINT watchlist_stocks_pkey TO watchlist_companies_pkey;
ALTER TABLE watchlist_companies
    RENAME CONSTRAINT watchlist_stocks_watchlist_fk TO watchlist_companies_watchlist_fk;
ALTER TABLE watchlist_companies
    RENAME CONSTRAINT watchlist_stocks_stock_fk TO watchlist_companies_company_fk;
ALTER INDEX watchlist_stocks_stock_id_idx RENAME TO watchlist_companies_company_id_idx;

ALTER TABLE price_snapshots RENAME COLUMN stock_id TO company_id;
ALTER TABLE price_snapshots
    RENAME CONSTRAINT price_snapshots_stock_fk TO price_snapshots_company_fk;
ALTER TABLE price_snapshots
    RENAME CONSTRAINT price_snapshots_stock_source_as_of_uk
    TO price_snapshots_company_source_as_of_uk;
ALTER INDEX price_snapshots_stock_as_of_idx RENAME TO price_snapshots_company_as_of_idx;

ALTER TABLE documents RENAME COLUMN stock_id TO company_id;
ALTER TABLE documents RENAME CONSTRAINT documents_stock_fk TO documents_company_fk;
ALTER INDEX documents_stock_publication_date_idx RENAME TO documents_company_publication_date_idx;

ALTER TABLE alerts RENAME COLUMN stock_id TO company_id;
ALTER TABLE alerts RENAME CONSTRAINT alerts_stock_fk TO alerts_company_fk;
ALTER INDEX alerts_stock_id_idx RENAME TO alerts_company_id_idx;
