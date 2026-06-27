# Enterprise Source Intelligence Platform

MarketMind separates source governance, connector execution, discovery, and
ingestion:

1. The Source Registry owns approved provider metadata and priority.
2. Source Intelligence selects a connector and records trust, freshness,
   formats, coverage, crawl metrics, and activity.
3. Discovery stores candidate document metadata without downloading content.
4. Pipeline orchestration performs approved ingestion and AI preparation.

## Connector Selection

`SourceConnector` is the discovery-side port. Implementations cover NSE, BSE,
SEBI, RBI, company investor-relations sites, RSS feeds, generic REST payloads,
and deterministic test documents.

The factory scores compatible connectors and adds a trust bonus so official
sources win when candidates are otherwise equivalent. URL hints allow RSS and
REST connectors to outrank generic HTML when appropriate.

NSE remains intentionally partial: the connector provides official-source
identity and diagnostics but does not implement browser automation or private
exchange APIs.

## Capability Detection

Supported formats combine connector declarations with safe URL inference:
PDF, RSS, JSON, XML, REST, GraphQL, HTML, and ZIP. Validation and refresh
operations persist detected profiles without storing credentials.

## APIs

All endpoints are additive under `/api/v1/source-intelligence`:

- `GET /catalog` and `/catalog/{sourceId}`
- `GET /health`, `/metrics`, `/activity`, `/coverage`
- `GET /connectors`, `/formats`
- `POST /sources/{sourceId}/validate`
- `POST /sources/{sourceId}/refresh`

Existing source, discovery, scheduler, and pipeline APIs remain unchanged.

Refresh and validation emit structured logs, Micrometer counters, and activity
records. The UI and global notification center surface filings, source
failures, discovery outcomes, and pipeline events.

Migration `V19__enterprise_source_platform.sql` is used because V17 and V18
already belong to pipeline orchestration and discovery diagnostics.
