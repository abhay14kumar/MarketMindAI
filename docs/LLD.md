# MarketMind AI — Low-Level Design

**Status:** Draft for Phase 1

**Purpose:** Define planned modules, core data entities, interfaces, workflows, and implementation constraints without creating application code.

## 1. Scope and Conventions

This design refines the service boundaries in [HLD.md](HLD.md). Names are provisional and should be validated when implementation begins.

Conventions:

- UUIDs are preferred for externally visible entity identifiers.
- Timestamps are stored in UTC and serialized as ISO 8601.
- Money is represented by decimal value plus ISO 4217 currency.
- Percentages and ratios use documented decimal conventions.
- Every market-sensitive record exposes `as_of`.
- Every AI artifact exposes model, prompt, retrieval, and policy versions.
- User-owned records include an ownership/tenant boundary.

## 2. Frontend Design

### 2.1 Proposed modules

```text
frontend/src/
├── app/              # Bootstrap, providers, router, global error handling
├── features/
│   ├── auth/
│   ├── companies/
│   ├── documents/
│   ├── research/
│   ├── watchlists/
│   └── portfolios/
├── components/       # Shared accessible UI components
├── api/              # Typed API client and transport concerns
├── hooks/            # Reusable application hooks
├── types/            # Shared frontend types
├── utils/            # Pure formatting and helper functions
└── test/             # Test setup and fixtures
```

### 2.2 State boundaries

- Server state should use a dedicated query/cache abstraction.
- Local UI state remains near the owning component.
- Authentication state is centralized but tokens are not persisted in insecure browser storage.
- Authoritative financial calculations are received from the backend, not recomputed independently in UI code.

### 2.3 Required UI states

Research and portfolio screens must display:

- loading, partial, empty, stale, failure, and permission-denied states;
- data source and `as_of`;
- citations and source navigation;
- assumptions and confidence/uncertainty;
- non-advisory and SEBI disclaimers near decision-support output.

## 3. Backend Design

### 3.1 Package-by-feature structure

```text
backend/src/main/java/.../marketmind/
├── common/           # Cross-cutting primitives, errors, time, money
├── identity/         # Current user, roles, authorization policies
├── company/          # Companies, instruments, provider mappings
├── document/         # Document metadata and ingestion requests
├── marketdata/       # Prices, fundamentals, provider adapters
├── portfolio/        # Portfolios, transactions, holdings, metrics
├── watchlist/        # Watchlists and monitored instruments
├── research/         # Research jobs, reports, citations, feedback
├── integration/      # AI service and external integration clients
└── audit/            # Security and business audit events
```

Each feature uses inward dependencies:

```text
web/API adapter -> application use case -> domain -> repository/provider ports
                                             ^
                               infrastructure adapters implement ports
```

Controllers do not contain business rules. JPA entities are not exposed as API contracts. Transactions are bounded to application use cases.

### 3.2 Core backend use cases

- Search and retrieve companies/instruments
- Register and inspect source documents
- Create/retry ingestion jobs
- Create and manage watchlists
- Create portfolios and record transactions
- Rebuild holdings and calculation snapshots
- Create/cancel/read research jobs
- Retrieve reports and citation metadata
- Capture user feedback

### 3.3 Portfolio calculation rules

The backend owns deterministic calculations including:

- quantity and cost basis;
- realized and unrealized gain/loss;
- position and sector allocation;
- concentration;
- return calculations;
- selected risk metrics once formulas are approved.

Each output records formula version, input snapshot, valuation timestamp, currency handling, and missing-data flags. Decimal arithmetic must be used for money. Calculation methodology requires tests against independent fixtures.

## 4. AI Service Design

### 4.1 Proposed modules

```text
ai-service/app/
├── api/              # Internal FastAPI routes and schemas
├── core/             # Configuration, logging, policies, shared types
├── ingestion/        # Acquisition, parsing, chunking, indexing
├── retrieval/        # Query planning, filters, search, reranking
├── agents/           # Specialist agents and CIO orchestration
├── llm/              # Ollama chat and embedding adapters
├── citations/        # Claim/citation mapping and validation
├── evaluation/       # Offline and online quality evaluation
├── clients/          # Backend, Qdrant, and source clients
└── workers/          # Asynchronous job handlers
```

### 4.2 Internal endpoints

Initial logical endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/internal/v1/ingestion-jobs` | Start an idempotent document ingestion |
| `GET` | `/internal/v1/ingestion-jobs/{id}` | Read ingestion state |
| `POST` | `/internal/v1/research-jobs` | Start a bounded research workflow |
| `GET` | `/internal/v1/research-jobs/{id}` | Read AI workflow status/result metadata |
| `POST` | `/internal/v1/index/rebuild` | Authorized rebuild of selected derived vectors |
| `GET` | `/internal/v1/health` | Service readiness metadata |

These routes are private, authenticated service-to-service, and unavailable to the browser.

## 5. Data Model

### 5.1 Identity and user data

| Entity | Important fields |
| --- | --- |
| `user_account` | `id`, external identity subject, status, locale, created/updated timestamps |
| `user_preference` | `user_id`, risk-display preferences, base currency, notification preferences |

### 5.2 Company and market data

| Entity | Important fields |
| --- | --- |
| `company` | `id`, legal name, sector, industry, country, website |
| `instrument` | `id`, `company_id`, exchange, ticker, ISIN, type, currency, active status |
| `provider_mapping` | internal entity ID, provider, provider key, valid period |
| `price_observation` | instrument, timestamp, OHLC/close as available, currency, source, `as_of` |
| `fundamental_observation` | company, metric code, value, unit, currency, period, source |

Uniqueness and revision policy must account for provider corrections.

### 5.3 Documents and chunks

| Entity | Important fields |
| --- | --- |
| `source_document` | `id`, company, type, title, source URL, publisher, publication date, reporting period, checksum, visibility, status |
| `document_version` | document, content checksum, parser version, storage reference, page count |
| `document_chunk` | `id`, version, ordinal, section, page start/end, text checksum, token estimate |
| `ingestion_job` | `id`, document/version, idempotency key, state, attempts, error code, timestamps |

Chunk vectors live in Qdrant and reference `document_chunk.id`.

### 5.4 Portfolio

| Entity | Important fields |
| --- | --- |
| `portfolio` | `id`, owner, name, base currency, benchmark, status |
| `portfolio_transaction` | portfolio, instrument, type, trade date, quantity, price, fees, currency |
| `holding_snapshot` | portfolio, instrument, quantity, cost basis, market value, `as_of`, calculation version |
| `portfolio_metric` | portfolio, metric code, value, period/`as_of`, formula version |
| `watchlist` | `id`, owner, name |
| `watchlist_item` | watchlist, instrument, notes, created timestamp |

Transactions are append-oriented. Corrections should preserve audit history rather than silently replacing prior values.

### 5.5 Research and evidence

| Entity | Important fields |
| --- | --- |
| `research_job` | owner, question, scope, state, request policy version, timestamps |
| `agent_run` | job, agent role, model, prompt version, state, latency, error code |
| `evidence_item` | job/agent, source type, source reference, excerpt hash, relevance, `as_of` |
| `research_report` | job, response schema version, answer, decision-support label, confidence band, disclaimer version |
| `report_citation` | report, claim/reference key, document/chunk/location, citation validation state |
| `research_feedback` | report, owner, rating/category, comment, timestamp |

Do not store unrestricted chain-of-thought. Store structured evidence, concise rationale, decisions, and operational metadata required for audit and evaluation.

## 6. State Machines

### 6.1 Ingestion job

```text
QUEUED -> ACQUIRING -> VALIDATING -> PARSING -> CHUNKING
       -> EMBEDDING -> INDEXING -> COMPLETED

Any processing state -> RETRYABLE_FAILED -> QUEUED
Any processing state -> PERMANENT_FAILED
QUEUED -> CANCELLED
```

Completion requires canonical metadata and all expected vector upserts. Partial indexing must not appear complete.

### 6.2 Research job

```text
QUEUED -> RETRIEVING -> ANALYZING -> SYNTHESIZING
       -> VALIDATING_CITATIONS -> COMPLETED

Any active state -> FAILED
QUEUED/active -> CANCELLED
VALIDATING_CITATIONS -> NEEDS_REVIEW or FAILED
```

## 7. Qdrant Collection Design

Initial logical collection: `document_chunks_v1`.

Recommended payload:

- `chunk_id`
- `document_id`
- `document_version_id`
- `company_id`
- `instrument_ids`
- `document_type`
- `publisher`
- `publication_date`
- `fiscal_period`
- `page_start`, `page_end`, `section`
- `visibility_scope`
- `language`
- `parser_version`
- `embedding_model`
- `embedding_version`
- `text_checksum`

Queries must apply authorization/visibility and entity filters before accepting results. Collection aliases should support safe reindexing during embedding migrations.

## 8. Research Contracts

### 8.1 Research request

A research request contains:

- user and authorization context supplied by backend;
- question;
- company/instrument/portfolio scope;
- allowed document types and date range;
- response mode;
- maximum work budget and timeout;
- required disclaimer policy.

### 8.2 Agent evidence packet

Each specialist returns a structured packet:

- agent role and version;
- findings;
- supporting evidence IDs;
- calculations and assumptions;
- risks or counter-evidence;
- missing information;
- confidence band;
- data `as_of`.

### 8.3 Final report

The CIO output includes:

- executive summary;
- optional non-binding label: buy/watch/hold/rotate review;
- thesis and counter-thesis;
- key financial and valuation observations;
- portfolio impact when authorized;
- risks and catalysts;
- assumptions and unresolved questions;
- confidence/uncertainty;
- source citations and timestamps;
- SEBI/non-advisory disclaimer.

The label must be omitted when evidence quality, freshness, or coverage falls below policy.

## 9. Error Handling

Public errors follow [API_GUIDELINES.md](API_GUIDELINES.md). Internal exceptions map to stable codes, for example:

- `SOURCE_NOT_APPROVED`
- `DOCUMENT_UNREADABLE`
- `DUPLICATE_DOCUMENT`
- `MARKET_DATA_STALE`
- `INSUFFICIENT_EVIDENCE`
- `MODEL_UNAVAILABLE`
- `RETRIEVAL_UNAVAILABLE`
- `CITATION_VALIDATION_FAILED`
- `PORTFOLIO_ACCESS_DENIED`

Logs may include correlation and job IDs but not secrets, authentication tokens, or unnecessary document/portfolio content.

## 10. Idempotency and Concurrency

- Document ingestion idempotency key: source identity + document checksum + parser/embedding version.
- Research submission accepts a client idempotency key for safe retries.
- Portfolio mutations use optimistic locking or equivalent conflict detection.
- Job workers claim work with leases and tolerate duplicate delivery.
- Qdrant upserts use stable point IDs derived from versioned chunk IDs.

## 11. Configuration

Configuration is environment-specific and validated at startup:

- PostgreSQL connection
- Qdrant endpoint and collection alias
- Ollama endpoint and model names
- backend/AI service credentials
- source-provider settings
- job limits, timeouts, chunking, and retrieval policy
- logging and telemetry settings

Secrets are injected at runtime and never committed. Safe defaults and `.env.example` may document variable names only.

## 12. Observability

Common fields:

- timestamp, severity, service, environment;
- trace ID, correlation ID, request/job ID;
- actor/service identity where appropriate;
- operation, outcome, latency, stable error code.

AI-specific metrics:

- ingestion throughput and failure rates;
- chunk and vector counts;
- retrieval latency and hit counts;
- model latency and context size;
- agent completion/failure;
- citation coverage and validation failure;
- abstention and user feedback rates.

## 13. Implementation Sequence

1. Define schemas and API contracts.
2. Implement identity boundaries and company/document metadata.
3. Implement ingestion state machine and document storage abstraction.
4. Implement chunking, embeddings, and Qdrant indexing.
5. Implement grounded single-agent research.
6. Add deterministic financial and portfolio calculations.
7. Introduce specialist agents and CIO synthesis behind feature flags.
8. Add evaluation gates before enabling decision-support labels.

## 14. Related Documents

- [High-Level Design](HLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [API Guidelines](API_GUIDELINES.md)
- [Coding Standards](CODING_STANDARDS.md)
- [Testing Strategy](TESTING_STRATEGY.md)
