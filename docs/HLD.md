# MarketMind AI — High-Level Design

**Status:** Draft for Phase 1

**Architecture style:** Modular services in a monorepo, deployed locally with Docker Compose

## 1. Purpose

This document defines the major runtime components, service boundaries, data flows, trust boundaries, and quality attributes for MarketMind AI. Detailed modules and interfaces are defined in [LLD.md](LLD.md); AI behavior is defined in [AI_RAG_DESIGN.md](AI_RAG_DESIGN.md).

## 2. Architectural Principles

- **Decision support, never autonomous trading:** no component places orders or automatically rebalances a portfolio.
- **Evidence before synthesis:** material claims must trace to a source or deterministic calculation.
- **Deterministic where possible:** Java/Python code performs calculations; the LLM explains and synthesizes.
- **Clear ownership:** PostgreSQL owns transactional metadata; Qdrant owns retrieval vectors; original documents remain in an object/document store abstraction.
- **Asynchronous heavy work:** ingestion, embedding, and multi-agent research are jobs, not long blocking requests.
- **Local-first development:** all required runtime dependencies can be orchestrated with Docker Compose.
- **Replaceable integrations:** external data providers and model adapters sit behind interfaces.
- **Secure by default:** least privilege, tenant isolation, source validation, and no secrets in source code.

## 3. Technology Baseline

| Layer | Technology |
| --- | --- |
| Frontend | React, TypeScript, Vite |
| Backend | Spring Boot 3, Java 21, Maven |
| AI service | Python, FastAPI |
| Relational database | PostgreSQL |
| Vector database | Qdrant |
| Local model runtime | Ollama |
| Initial chat model | `llama3.1` |
| Initial embedding model | `nomic-embed-text` |
| Local orchestration | Docker Compose |

Model identifiers are configuration, not hard-coded business logic. Model upgrades require evaluation before promotion.

## 4. System Context

```text
                                      Approved external sources
                           filings / transcripts / fundamentals / prices
                                              |
                                              v
+---------+       HTTPS        +-----------------------------+
|  User   | <----------------> | React + TypeScript frontend |
+---------+                     +--------------+--------------+
                                               |
                                               | REST/JSON
                                               v
                                +--------------+--------------+
                                | Spring Boot backend         |
                                | system API and domain logic |
                                +----+-----------+-------------+
                                     |           |
                     SQL/JDBC        |           | internal REST/jobs
                                     v           v
                              +------+----+  +---+------------------+
                              | PostgreSQL|  | Python FastAPI AI    |
                              +-----------+  | ingestion/RAG/agents |
                                             +---+------+-----------+
                                                 |      |
                                          vectors|      | model APIs
                                                 v      v
                                             +---+--+ +-------------+
                                             |Qdrant| | Ollama      |
                                             +------+ | llama3.1    |
                                                      | embeddings  |
                                                      +-------------+
```

For local development, these components share a Docker Compose network. Production topology is intentionally deferred.

## 5. Major Components

### 5.1 Frontend

Responsibilities:

- Authentication and user session experience
- Company search and research workspace
- Document viewer with citation navigation
- Portfolio, holdings, allocation, and risk views
- Long-running analysis status and streaming/polling presentation
- Clear rendering of source dates, assumptions, uncertainty, and disclaimers

The frontend contains presentation logic only. It does not calculate authoritative financial metrics or call Ollama/Qdrant directly.

### 5.2 Spring Boot backend

The backend is the public system API and primary domain layer.

Responsibilities:

- Authentication integration and authorization
- User, company, watchlist, portfolio, holding, and transaction domains
- Market/fundamental provider adapters and normalized data
- Deterministic portfolio and financial calculations
- Research request lifecycle and audit metadata
- Validation, quotas, rate limits, and API contracts
- Coordination with the AI service through internal APIs/jobs

The backend is the sole public entry point for the initial product. The AI service is not exposed directly to browsers.

### 5.3 FastAPI AI service

Responsibilities:

- Document acquisition workflow and parsing coordination
- Chunking, embedding, and Qdrant indexing
- Query understanding, retrieval, reranking, and context assembly
- Specialist-agent execution and CIO synthesis
- Citation validation and grounded answer formatting
- AI evaluation hooks, prompt versions, and model metadata

The AI service must not own user authentication, portfolio transaction truth, or order execution.

### 5.4 PostgreSQL

Stores:

- Users and authorization-related application records
- Companies, instruments, and source-provider mappings
- Document metadata and ingestion state
- Market/fundamental normalized records as applicable
- Portfolios, transactions, holdings, and calculation snapshots
- Research jobs, answer metadata, citations, feedback, and audit events

PostgreSQL is the system of record for application state. Large binary documents should use a storage abstraction rather than database blobs when implementation begins.

### 5.5 Qdrant

Stores document-chunk vectors and retrieval payloads. Payloads include stable references to PostgreSQL document/chunk metadata, tenant visibility, company, document type, reporting period, page/section, source, and embedding version.

Qdrant is a derived index and can be rebuilt from canonical documents and metadata.

### 5.6 Ollama

Provides the initial local inference runtime:

- `llama3.1` for generation and agent reasoning
- `nomic-embed-text` for embeddings

Ollama is reachable only by the AI service on the private network. Timeouts, concurrency, and model availability must be monitored.

### 5.7 External source adapters

Adapters isolate provider-specific authentication, formats, rate limits, licensing constraints, retries, and data normalization. A source registry records which providers and document types are approved.

## 6. Primary Data Flows

### 6.1 Document ingestion

1. A scheduled or manual request identifies a source document.
2. The backend or AI ingestion adapter validates the source against the approved registry.
3. Document metadata and an idempotency key are recorded in PostgreSQL.
4. Content is downloaded, scanned/validated, checksummed, and parsed.
5. The AI service creates section-aware chunks with location metadata.
6. `nomic-embed-text` creates vectors through Ollama.
7. Vectors and filtered payloads are upserted into Qdrant.
8. PostgreSQL records completion, counts, parser version, and embedding version.

### 6.2 Research question

1. Frontend sends the question and scope to the backend.
2. Backend authorizes access, validates scope, creates a research job, and sends a bounded request to the AI service.
3. AI service retrieves relevant chunks using metadata filters and semantic search.
4. Structured financial/portfolio context is obtained through approved backend interfaces.
5. Relevant specialist agents produce structured evidence packets.
6. CIO Agent synthesizes the final report.
7. Citation validation checks that material claims map to supplied evidence.
8. Backend persists answer metadata and returns/streams the result to the frontend.

### 6.3 Portfolio review

1. User changes a transaction or requests a review.
2. Backend recalculates holdings and metrics deterministically.
3. Backend sends a minimal portfolio snapshot and selected company context to the AI service.
4. Portfolio and Risk Agents assess exposure; other agents run only when required.
5. CIO Agent returns non-binding review priorities and evidence.

## 7. Trust Boundaries

- Browser to backend: untrusted public input
- Backend to AI service: authenticated internal traffic but still validated
- External sources to ingestion pipeline: untrusted content
- Retrieved chunks to prompts: untrusted evidence, never system instructions
- User-uploaded documents: private and potentially malicious
- PostgreSQL/Qdrant/Ollama: private network services, not publicly exposed

See [SECURITY.md](SECURITY.md) for controls.

## 8. Data Ownership and Consistency

| Data | Owner | Notes |
| --- | --- | --- |
| User and portfolio state | Backend/PostgreSQL | Strong transactional consistency |
| Document metadata | PostgreSQL | Canonical provenance and status |
| Original document | Document storage abstraction | Immutable/checksummed where possible |
| Chunk text metadata | PostgreSQL or canonical artifact store | Rebuild source for vector index |
| Embeddings | Qdrant | Derived and versioned |
| Prompt templates | Version-controlled `prompts/` | No secrets or user data |
| AI answer metadata | PostgreSQL | Includes model, prompt, retrieval, and citation versions |

Cross-service workflows use idempotency keys and explicit statuses. Distributed transactions are avoided; retries and reconciliation jobs handle eventual consistency.

## 9. API and Integration Style

- Public and internal interfaces use versioned REST/JSON initially.
- OpenAPI specifications are generated and reviewed.
- Long tasks return `202 Accepted` with a job resource.
- All requests carry a correlation ID; mutating requests support idempotency where retries are expected.
- Dates use ISO 8601; money includes currency; market data includes `as_of`.
- Internal failures use stable error codes and do not expose secrets or stack traces.

See [API_GUIDELINES.md](API_GUIDELINES.md).

## 10. Quality Attributes

### Security and privacy

Tenant checks occur at every user-owned resource boundary. Sensitive data is minimized. Secrets use environment injection or secret management and are never committed.

### Reliability

Jobs use explicit state machines, bounded retries, dead-letter handling, and idempotent operations. Qdrant can be rebuilt without loss of canonical records.

### Performance

Interactive reads use pagination and indexes. Expensive parsing and generation run asynchronously. Retrieval limits context size and filters before semantic search.

### Observability

Services emit structured logs, metrics, and traces with correlation IDs. AI telemetry records latency, token/context estimates, retrieval counts, citation coverage, and model/prompt versions while avoiding raw sensitive content by default.

### Scalability

The initial deployment is Docker Compose. Service boundaries permit later independent scaling of the backend, ingestion workers, AI workers, Qdrant, and databases without requiring premature microservices.

## 11. Failure and Degradation Strategy

| Failure | Expected behavior |
| --- | --- |
| Market provider unavailable | Serve clearly timestamped cached data or report unavailability |
| Document parsing fails | Mark failed with reason; do not index partial content as complete |
| Qdrant unavailable | Research requiring retrieval fails safely; portfolio CRUD remains available |
| Ollama/model unavailable | Queue/retry within policy or return a transparent unavailable status |
| Insufficient evidence | Abstain or provide a limited answer with missing evidence listed |
| Agent fails | CIO report identifies unavailable analysis; no invented substitute |
| Citation validation fails | Do not publish the unsupported final answer |

## 12. Deployment View

The initial `docker-compose.yml` is expected to define:

- `frontend`
- `backend`
- `ai-service`
- `postgres`
- `qdrant`
- `ollama`
- optional one-shot model initialization and database migration services

Only the frontend and backend should require host exposure. Persistent named volumes protect PostgreSQL, Qdrant, Ollama models, and local document artifacts. Health checks and dependency readiness are required.

## 13. Key Decisions and Deferred Items

Accepted:

- React + TypeScript + Vite frontend
- Spring Boot 3 + Java 21 + Maven backend
- Python FastAPI AI service
- PostgreSQL and Qdrant
- Ollama with `llama3.1` and `nomic-embed-text`
- Docker Compose for initial development

Deferred:

- Production cloud and orchestration
- Event broker selection
- Identity provider
- Object storage implementation
- Live market-data and transcript providers
- Reranking model and OCR provider

## 14. Related Documents

- [Product Requirements](PRD.md)
- [Low-Level Design](LLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [ADR-001](ADR-001-architecture-style.md)
- [Security](SECURITY.md)
