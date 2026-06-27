# MarketMind AI Backend

Spring Boot foundation for the MarketMind AI public backend API.

Enterprise source operations are available under
`/api/v1/source-intelligence`. The trust-aware connector platform supports
NSE, BSE, SEBI, RBI, company IR, RSS, REST, and test sources while preserving
existing APIs. See `docs/SOURCE_INTELLIGENCE_PLATFORM.md`.

## Technology

- Java 21
- Spring Boot 3.5.15
- Maven
- Spring Web and Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Boot Actuator

## Current Scope

The backend currently includes:

- company master CRUD;
- market-data and scheduler foundations;
- document source and acquisition metadata;
- generic HTTP/HTTPS document downloading;
- SHA-256 duplicate detection and immutable document versions;
- local filesystem storage with PostgreSQL-backed metadata;
- Zerodha holdings XLSX import with portfolio snapshots and allocation analytics;
- manual and deterministic mock price snapshots for portfolio valuation;
- global problem-details exception handling and health endpoints.

Source-specific browser/session automation and authenticated provider APIs
remain out of scope; current official connectors use safe HTTP discovery with
explicit diagnostics.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL

Verify the toolchain:

```bash
java -version
mvn -version
```

## Database Setup

The default backend configuration matches the local PostgreSQL service defined by `docker/.env.example`:

```text
URL:      jdbc:postgresql://127.0.0.1:5432/marketmind
Username: marketmind_user
Password: marketmind_pass
```

These predictable values are development-only. Shared and production environments must override them through environment variables or an approved secrets manager:

```bash
export DB_URL='jdbc:postgresql://database-host:5432/marketmind'
export DB_USERNAME='runtime_database_user'
read -s DB_PASSWORD
export DB_PASSWORD
```

Optional variables:

```bash
export SERVER_PORT='8080'
export CORS_ALLOWED_ORIGINS='http://localhost:5173,http://localhost:5175'
export DB_MAX_POOL_SIZE='10'
export DB_MIN_IDLE='2'
export DB_CONNECTION_TIMEOUT_MS='30000'
export DOCUMENT_STORAGE_ROOT_PATH='data/documents'
export DOCUMENT_DOWNLOAD_TIMEOUT_SECONDS='30'
export DOCUMENT_DOWNLOAD_MAX_FILE_SIZE_MB='50'
export SOURCE_VALIDATION_CONNECT_TIMEOUT_SECONDS='10'
export SOURCE_VALIDATION_READ_TIMEOUT_SECONDS='15'
export SOURCE_VALIDATION_MAX_PDF_TEST_SIZE_MB='10'
export SOURCE_VALIDATION_USER_AGENT='MarketMindAI-SourceValidator/1.0'
export PORTFOLIO_IMPORT_MAX_FILE_SIZE='10MB'
export PORTFOLIO_IMPORT_MAX_REQUEST_SIZE='10MB'
export MARKET_PRICE_PROVIDER='PUBLIC'
export MARKET_PRICE_REFRESH_ENABLED='false'
export MARKET_PRICE_REFRESH_INTERVAL_SECONDS='60'
export MARKET_PRICE_PROVIDER_TIMEOUT_SECONDS='10'
```

`CORS_ALLOWED_ORIGINS` accepts a comma-separated list.

The backend currently has no Spring Security dependency or authentication
filter chain. During local development, `/api/v1/**` is publicly accessible and
browser requests from the configured frontend origins are allowed, including
OPTIONS preflight requests. This is temporary until the authentication module
is implemented.

## Run Locally

First start the local infrastructure. From `backend/`:

```bash
docker compose --env-file ../docker/.env up -d
```

Then start Spring Boot:

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration` during startup. Hibernate validates the migrated schema and does not create or update tables.
The local profile also writes the rolling file
`logs/marketmind-backend.log` for Promtail collection.

Verify database-backed application health:

```bash
curl http://localhost:8080/actuator/health
```

## Build and Test

```bash
mvn clean verify
```

## Document Download Pipeline

The generic download endpoint accepts public HTTP or HTTPS URLs:

```bash
curl -X POST http://localhost:8080/api/v1/documents/download \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUrl": "https://example.com/annual-report.pdf",
    "title": "Example Company Annual Report",
    "documentType": "ANNUAL_REPORT",
    "companyId": null,
    "sourceId": null,
    "fiscalYear": 2026,
    "quarter": null
  }'
```

The synchronous pipeline:

1. records a `STARTED` download job;
2. streams the response to a bounded temporary file;
3. calculates a SHA-256 checksum and rejects duplicates;
4. stores the file under
   `data/documents/{yyyy}/{MM}/{checksum}/{original-file-name}`;
5. persists document metadata and an immutable version;
6. marks the job `COMPLETED` or `FAILED`.

Files under `data/documents/` are local runtime data and are ignored by Git.
Production deployments should mount durable storage and override
`DOCUMENT_STORAGE_ROOT_PATH`.

Available document APIs:

```text
POST /api/v1/documents/download
GET  /api/v1/documents
GET  /api/v1/documents/{id}
GET  /api/v1/documents/jobs
GET  /api/v1/documents/versions/{documentId}
```

Security controls include protocol allowlisting, timeout and file-size limits,
bounded redirects, private/local network rejection, safe filenames, and
path-containment checks. Generic downloading should still be exposed only to
authorized users when authentication is introduced.

## Source Registry Validation

Register or update a source with an optional public sample PDF URL:

```bash
curl -X POST http://localhost:8080/api/v1/sources \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "EXAMPLE_IR",
    "name": "Example Investor Relations",
    "organization": "Example Company",
    "description": "Example company investor relations source.",
    "sourceType": "COMPANY_INVESTOR_RELATIONS",
    "status": "ACTIVE",
    "authenticationType": "NONE",
    "refreshFrequency": "DAILY",
    "baseUrl": "https://example.com",
    "robotsUrl": "https://example.com/robots.txt",
    "documentationUrl": "https://example.com/investors",
    "samplePdfUrl": "https://example.com/investors/sample-report.pdf",
    "capabilities": ["INVESTOR_RELATIONS_DOCUMENTS"],
    "enabled": true,
    "priority": 50,
    "reliabilityScore": 0.9000
  }'
```

Run generic HTTP validation for a registered source:

```bash
curl -X POST http://localhost:8080/api/v1/sources/{sourceId}/validate
```

Inspect recorded source health:

```bash
curl http://localhost:8080/api/v1/sources/health
```

Validation performs a bounded HTTP reachability check, records latency, checks
the origin `/robots.txt`, and inspects an optional sample PDF by content type or
the `%PDF-` signature. HEAD requests fall back to GET when unsupported.
Redirects are limited to three and response-body inspection is bounded. Each
check runs independently: complete success is recorded as `SUCCESS`, partial
evidence as `WARNING`, and `FAILED` only when none of the key checks succeeds.
Source failures are persisted and never stop the application.

No credentials or API keys are accepted or stored by source validation.

## Portfolio Intelligence

Export holdings from Zerodha Console or Kite as XLSX, then import the file
without providing broker credentials:

```bash
curl -X POST http://localhost:8080/api/v1/portfolio/import \
  -H 'Accept: application/json' \
  -F 'file=@/absolute/path/to/holdings.xlsx'
```

The workbook is parsed in memory with Apache POI and is not stored. Headers are
matched dynamically, optional values are derived when possible, and invalid
rows are recorded on the import job without rejecting otherwise valid rows.
Each successful import atomically replaces the current Zerodha holdings and
creates a point-in-time portfolio snapshot.

Available portfolio APIs:

```text
POST /api/v1/portfolio/import
GET  /api/v1/portfolio/summary
GET  /api/v1/portfolio/holdings
GET  /api/v1/portfolio/allocation/sector
GET  /api/v1/portfolio/allocation/instrument
GET  /api/v1/portfolio/import-jobs
```

## Price Feed Foundation

The local price feed stores manual or deterministic mock snapshots. It performs
no external API calls and requires no broker credentials.

```bash
curl -X POST http://localhost:8080/api/v1/market/prices/manual \
  -H 'Content-Type: application/json' \
  -d '{
    "symbol": "INFY",
    "exchange": "NSE",
    "lastPrice": 1600.00,
    "previousClose": 1580.00,
    "source": "MANUAL"
  }'

curl -X POST http://localhost:8080/api/v1/market/prices/mock-refresh

curl http://localhost:8080/api/v1/market/prices/latest
```

Mock refresh uses existing portfolio holdings and creates bounded,
deterministic movements around each imported previous close. These values are
for UI and calculation testing only; they are not live market data or trading
recommendations.

### Public Price Provider

The `PUBLIC` provider is a best-effort, credential-free adapter for Yahoo
Finance's chart response. Indian NSE symbols are mapped to `.NS` symbols, for
example `INFY` to `INFY.NS`. The adapter uses bounded HTTP/1.1 requests and
continues refreshing other holdings when one symbol fails.

```bash
curl -X POST http://localhost:8080/api/v1/market/prices/refresh-real

curl http://localhost:8080/api/v1/market/prices/provider-status
```

Scheduled refresh is disabled by default. Enable it only when the local
environment should make outbound quote requests:

```bash
export MARKET_PRICE_REFRESH_ENABLED='true'
export MARKET_PRICE_REFRESH_INTERVAL_SECONDS='60'
```

Public endpoints can rate-limit, change, or become unavailable without notice.
Provider failures are recorded in `price_feed_job`, do not stop the backend,
and never trigger trading actions.

## PDF Text Extraction

Stored PDF versions can be parsed with Apache PDFBox:

```bash
curl -X POST \
  http://localhost:8080/api/v1/documents/{documentId}/extract-text

curl \
  http://localhost:8080/api/v1/documents/{documentId}/extracted-text
```

Extraction records include the document version, status, extracted text, page
count, character count, timestamps, and a bounded error message. Only PDF is
supported. Image-only or scanned PDFs are marked `UNSUPPORTED` when no text
layer is found because OCR is intentionally not included yet.

Extracted text is document content and may contain sensitive information.
Apply access control before exposing these APIs outside local development.
OCR remains out of scope.

## Local RAG Pipeline

MarketMind can chunk completed PDF text extractions, generate embeddings with
local Ollama, store vectors in Qdrant, and answer source-grounded questions.
The implementation uses `nomic-embed-text` for embeddings and `llama3.1` for
chat generation. No paid AI API or broker integration is used.

Install and start the local models:

```bash
ollama pull nomic-embed-text
ollama pull llama3.1
ollama serve
```

Start PostgreSQL and Qdrant from the repository root, then run the backend:

```bash
docker compose --env-file docker/.env up -d

cd backend
mvn spring-boot:run
```

Extract a downloaded PDF before indexing it:

```bash
curl -X POST \
  http://localhost:8080/api/v1/documents/{documentId}/extract-text

curl -X POST \
  http://localhost:8080/api/v1/ai/documents/{documentId}/embed

curl \
  http://localhost:8080/api/v1/ai/documents/{documentId}/chunks
```

Ask across all indexed documents, or include `documentId` to restrict
retrieval:

```bash
curl -X POST http://localhost:8080/api/v1/ai/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "What material risks are described in the report?",
    "topK": 5
  }'

curl http://localhost:8080/api/v1/ai/answers
```

Local defaults are configurable through environment variables:

```text
OLLAMA_HOST=http://localhost:11434
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
OLLAMA_CHAT_MODEL=llama3.1
QDRANT_URL=http://localhost:6333
QDRANT_API_KEY=
QDRANT_COLLECTION=marketmind_documents
CHUNK_SIZE=1000
CHUNK_OVERLAP=200
TOP_K_RESULTS=5
```

When using the repository Docker configuration, set `QDRANT_API_KEY` to the
local value from `docker/.env`. Never place production API keys in source
control. Answers are generated only from retrieved chunks, include citations,
and carry this disclaimer: “AI answer is based only on indexed documents and
is not financial advice.” If relevant context is unavailable, the API returns
`INSUFFICIENT_CONTEXT`.

Uploaded workbooks must remain local. The repository ignores `*.xlsx`; never
commit a broker export because it contains sensitive financial information.

### Seed and Test Data

Flyway seeds governed metadata for NSE, BSE, SEBI, RBI, AMFI, Yahoo Finance,
Finnhub, AlphaVantage, and a W3C validation fixture. Seed operations use
conflict-safe upserts and never include API keys.

The `W3C_TEST` source exists only to exercise generic validation reliably:

```text
Base URL:       https://www.w3.org
Robots URL:     https://www.w3.org/robots.txt
Sample PDF URL: https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf
```

Find and validate it:

```bash
curl 'http://localhost:8080/api/v1/sources?size=100'

curl -X POST \
  http://localhost:8080/api/v1/sources/71000000-0000-0000-0000-000000000009/validate
```

When outbound internet access is available, the expected result is reachable
HTTP, an available `robots.txt`, and `pdfCapabilityStatus` equal to
`SUPPORTED`. This fixture is test metadata, not a financial data provider.

## Health Checks

Application health:

```bash
curl http://localhost:8080/api/v1/health
```

Actuator health, including configured dependency indicators:

```bash
curl http://localhost:8080/actuator/health
```

Expected application response:

```json
{
  "status": "UP",
  "service": "marketmind-backend"
}
```

## Request Correlation and Structured Errors

Every HTTP request accepts `X-Correlation-Id` or the compatibility alias
`X-Request-ID`. Safe caller
values are reused; missing or unsafe values are replaced with a generated UUID.
Both headers are returned on the response.

```bash
curl -i \
  -H 'X-Correlation-Id: local-validation-001' \
  http://localhost:8080/actuator/health
```

The correlation ID is stored in SLF4J MDC for the request lifetime and appears
in the human-readable console pattern. Request logs include method, path,
status, duration, and correlation ID without request bodies, file contents, or
portfolio values:

```text
2026-06-21T07:00:00.000Z INFO [http-nio-8080-exec-1]
[correlationId=local-validation-001] CorrelationIdFilter -
HTTP request completed httpMethod="GET" httpPath="/actuator/health"
httpStatus="200" durationMs="12"
```

With `SPRING_PROFILES_ACTIVE=local`, the same logs are written to the rolling
file `logs/marketmind-backend.log`. The repository Promtail service collects
that file for Loki. See [monitoring/README.md](../monitoring/README.md).

API failures use RFC 9457 problem details with stable error codes:

```json
{
  "type": "https://docs.marketmind.local/problems/validation-failed",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/companies",
  "code": "VALIDATION_FAILED",
  "legacyCode": "VALIDATION_ERROR",
  "correlationId": "local-validation-001",
  "requestId": "local-validation-001",
  "timestamp": "2026-06-21T07:00:00Z",
  "fieldErrors": [
    {
      "field": "companyName",
      "code": "NotBlank",
      "message": "Company name is required."
    }
  ]
}
```

The legacy `errors` array remains as a temporary compatibility alias for
`fieldErrors`.

Malformed JSON, enum conversion, multipart failures, upload limits, external
provider failures, and Qdrant/Ollama outages have distinct machine-readable
codes. Dependency details and stack traces remain in server logs and are not
returned to clients.

To trace a request, search logs for the response's `X-Correlation-Id` value.
The console pattern can be overridden with `CONSOLE_LOG_PATTERN`; JSON logging
can later be introduced by replacing the Logback encoder without application
code changes.

## Automated Document Processing

Successful document downloads can automatically run PDF text extraction,
chunking, Ollama embedding, Qdrant indexing, and the final `AI_READY` state.
The feature is enabled by default and can be controlled with:

```bash
export DOCUMENT_PIPELINE_AUTO_PROCESS_ENABLED=true
```

Pipeline operations are available at:

- `GET /api/v1/pipeline/runs`
- `GET /api/v1/pipeline/runs/{id}`
- `GET /api/v1/pipeline/documents/{documentId}`
- `POST /api/v1/pipeline/documents/{documentId}/retry`

Manual extraction and embedding endpoints remain available. A failed automated
step is recorded without rolling back a successfully downloaded document.

## Document Discovery

The discovery engine scans trusted public HTML sources for PDF links and stores
only classified metadata. It does not download, extract, embed, or ingest any
discovered document.

Configuration:

```bash
export DISCOVERY_TIMEOUT='10s'
export DISCOVERY_MAX_HTML_BYTES='2097152'
export DISCOVERY_USER_AGENT='Mozilla/5.0 MarketMindAI-Discovery/1.0'
```

Use `POST /api/v1/discovery/run` with `TEST_SOURCE` for deterministic local
testing or `COMPANY_IR` with a public `sourceUrl` for generic HTML discovery.
NSE, BSE, SEBI, and RBI now have distinct connector identities and trust-aware
selection while reusing safe HTTP extraction internally. Source-specific
browser/session automation remains intentionally deferred.

## Autonomous Pipeline Orchestration

The production pipeline entry point coordinates existing discovery, download,
PDF extraction, chunking, Ollama embedding, Qdrant indexing, AI summarization,
and final `AI_READY` services. New discovery records automatically create a
pipeline job after the discovery transaction commits. Existing manual document
downloads enter orchestration at text extraction.

Each job has a correlation ID, durable stages and events, structured stage logs,
Micrometer counters/timers, and up to three attempts per stage with exponential
backoff.

Configuration:

```bash
export PIPELINE_ORCHESTRATION_ENABLED=true
export PIPELINE_MAX_ATTEMPTS=3
export PIPELINE_INITIAL_BACKOFF=250ms
export PIPELINE_SUMMARY_MAX_CHARACTERS=12000
```

APIs:

- `POST /api/v1/pipeline/start`
- `POST /api/v1/pipeline/jobs/{id}/retry`
- `GET /api/v1/pipeline/jobs`
- `GET /api/v1/pipeline/jobs/{id}`
- `GET /api/v1/pipeline/jobs/{id}/events`
- `GET /api/v1/pipeline/metrics`

Micrometer metrics are available through Actuator, including the Prometheus
endpoint. Existing `/api/v1/pipeline/runs` and document-pipeline endpoints are
retained for compatibility.

## Package Structure

```text
src/main/java/com/marketmind/
├── MarketMindApplication.java
├── common/
│   ├── exception/
│   └── observability/
├── config/
├── ai/
├── company/
├── documents/
├── discovery/
├── marketdata/
├── portfolio/
├── pipeline/
├── scheduler/
└── health/
    └── adapter/in/web/
```

Future features should follow clean architecture boundaries:

```text
web adapter -> application use case -> domain <- persistence/provider adapter
```

Domain and application layers must not depend on Spring MVC, JPA entities, or provider-specific implementations.

## Security

- Do not store database passwords, API keys, tokens, or other secrets in this repository.
- Use environment variables locally and an approved secret manager in shared environments.
- PostgreSQL should not be publicly exposed.
- The current backend intentionally has no authentication and is suitable only for local foundation work until access control is implemented.
