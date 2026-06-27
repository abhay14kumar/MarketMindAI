ALTER TABLE discovery_job
    ADD COLUMN ignored_documents INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN message VARCHAR(1000),
    ADD COLUMN recommendation VARCHAR(1000),
    ADD COLUMN crawler_type_used VARCHAR(100),
    ADD COLUMN source_reachable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN html_fetched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN links_scanned INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN pdf_links_found INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN reason_when_zero_results VARCHAR(2000);

ALTER TABLE discovery_source_run
    ADD COLUMN crawler_type VARCHAR(100),
    ADD COLUMN http_status INTEGER,
    ADD COLUMN fetched_html_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_links_found INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN pdf_links_found INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN skipped_links_count INTEGER NOT NULL DEFAULT 0;
