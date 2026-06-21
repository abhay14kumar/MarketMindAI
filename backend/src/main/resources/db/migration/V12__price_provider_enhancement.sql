ALTER TABLE price_feed_job
    ADD COLUMN provider VARCHAR(60),
    ADD COLUMN requested_symbols_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN successful_symbols_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN failed_symbols_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN error_summary TEXT;

ALTER TABLE price_feed_job
    ADD CONSTRAINT price_feed_job_provider_counts_check CHECK (
        requested_symbols_count >= 0
        AND successful_symbols_count >= 0
        AND failed_symbols_count >= 0
        AND successful_symbols_count + failed_symbols_count <= requested_symbols_count
    );

ALTER TABLE price_feed_job DROP CONSTRAINT price_feed_job_source_check;
ALTER TABLE price_feed_job ADD CONSTRAINT price_feed_job_source_check
    CHECK (source IN (
        'REAL', 'PUBLIC', 'MANUAL', 'MOCK', 'ZERODHA', 'YAHOO', 'FINNHUB', 'NSE'
    ));

ALTER TABLE price_snapshot DROP CONSTRAINT price_snapshot_source_check;
ALTER TABLE price_snapshot ADD CONSTRAINT price_snapshot_source_check
    CHECK (source IN (
        'REAL', 'PUBLIC', 'MANUAL', 'MOCK', 'ZERODHA', 'YAHOO', 'FINNHUB', 'NSE'
    ));
