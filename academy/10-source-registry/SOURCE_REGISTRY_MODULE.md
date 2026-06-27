# Discovery and Source Limitations

Source validation answers whether a source is reachable and suitable for
configured capabilities. Discovery is a separate operation that scans a page
for document links; it does not download or ingest those documents.

The generic HTML crawler follows redirects, uses browser-like headers, resolves
relative links, and records HTTP status, fetched bytes, links scanned, PDF
links found, and skipped links. It only sees links exposed in server-rendered
HTML.

NSE corporate-filings pages may be dynamic, session-protected, or backed by
source-specific APIs. A generic crawl can therefore complete with zero results.
This is not a silent failure: the API and UI return a warning and explain that
an NSE-specific crawler is planned. Browser automation is deliberately out of
scope.

## Enterprise Connector Architecture

The registry feeds a connector-based Source Intelligence Platform. Connectors
declare trust tier, formats, document types, and selection fitness. Official
connectors receive priority over third-party connectors when both can serve a
request. Operational profiles track trust, reliability, freshness, latency,
scheduler state, crawl outcomes, and document coverage.

The Sources Intelligence Center presents catalog, coverage matrix, live
discovery/pipeline activity, connector inventory, validation, and refresh
actions without weakening the registry's governance boundary.
