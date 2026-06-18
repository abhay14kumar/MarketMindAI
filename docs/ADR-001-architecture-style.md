# ADR-001: Modular Service Architecture in a Monorepo

- **Status:** Accepted for initial implementation
- **Date:** 2026-06-18
- **Decision owners:** MarketMind AI engineering

## Context

MarketMind AI combines conventional transactional product capabilities with document processing, retrieval-augmented generation, local model inference, financial calculations, and portfolio analysis.

The system needs:

- a browser-based React experience;
- strongly governed user, portfolio, and financial domains;
- Python AI/document tooling;
- PostgreSQL transactional persistence;
- Qdrant vector retrieval;
- Ollama local inference;
- reproducible local development through Docker Compose.

A single-language monolith would simplify deployment but would force either the product/backend domain or the AI ecosystem into a less suitable environment. A large set of fine-grained microservices would add network, deployment, observability, data-consistency, and operational cost before product boundaries and load patterns are proven.

## Decision

Adopt a **modular service architecture in a single monorepo** with three initial application deployables:

1. **Frontend:** React + TypeScript + Vite
2. **Backend:** Spring Boot 3 + Java 21 + Maven
3. **AI service:** Python FastAPI

Supporting stateful services:

- PostgreSQL as the application system of record;
- Qdrant as a rebuildable vector index;
- Ollama running `llama3.1` and `nomic-embed-text`;
- Docker Compose for initial local orchestration.

The Spring Boot backend is the public system API and owns identity integration, authorization, companies/instruments, market/fundamental normalization, portfolios, deterministic calculations, research-job lifecycle, and audit metadata.

The FastAPI AI service is private and owns document transformation, chunking, embeddings, retrieval, specialist-agent orchestration, CIO synthesis, citation validation, and AI evaluation.

Within each service, organize code as cohesive domain/feature modules with explicit ports and adapters. Do not split modules into separately deployed services until measured scaling, reliability, ownership, security, or release needs justify it.

Heavy workflows use asynchronous job semantics. The initial interface style is versioned REST/JSON; selection of an event broker is deferred.

## Decision-Support Boundary

No service will place trades or automatically rebalance a portfolio. Buy/watch/hold/rotate outputs are non-binding research labels with evidence, uncertainty, timestamps, risks, and disclaimers.

## Rationale

- Java/Spring provides a strong foundation for transactional domains, authorization, typed APIs, and deterministic financial logic.
- Python/FastAPI fits document, retrieval, model, and evaluation ecosystems.
- Separate AI runtime failures from core portfolio CRUD and calculations.
- Keep deployment count small enough for an early-stage team.
- Preserve future independent scaling of ingestion and AI workloads.
- A monorepo makes cross-service contracts, documentation, local setup, and coordinated changes easier during Phase 1–3.
- PostgreSQL remains canonical; Qdrant can be rebuilt, reducing dual-write risk.

## Consequences

### Positive

- Clear responsibility and trust boundaries
- Technology fit for each workload
- AI service can evolve without exposing model infrastructure publicly
- Core product remains available during some AI dependency failures
- Local stack is reproducible with Docker Compose
- Future extraction points are visible without premature microservices

### Negative

- Two backend languages and build ecosystems
- Network contracts and failure handling between backend and AI service
- Cross-service tracing, schema compatibility, and local resource usage are required
- Asynchronous consistency must be designed explicitly
- Docker Compose is not a production orchestration strategy

### Required controls

- OpenAPI/JSON Schema contracts and compatibility tests
- Correlation IDs and distributed tracing
- Idempotent jobs and explicit state machines
- Private AI/Qdrant/Ollama network exposure
- PostgreSQL ownership and Qdrant rebuild procedures
- Model/prompt/index versioning and evaluation gates
- Secret management and tenant-isolation tests

## Alternatives Considered

### Single Spring Boot application with embedded AI integration

Rejected for the initial design because Python has stronger document/RAG experimentation support, and model/ingestion workloads should fail and scale independently from transactional APIs.

### Single Python application

Rejected because the planned portfolio, authorization, financial, and integration domains benefit from the chosen Java 21/Spring Boot baseline and deterministic typed boundaries.

### Fine-grained microservices

Rejected for now because domain boundaries, team ownership, throughput, and operational requirements are not mature enough to justify the complexity.

### Serverless functions

Not selected as the primary style because document parsing, local Ollama inference, long jobs, and stateful local development do not fit a function-first model cleanly. Individual future integrations may still use serverless components after a separate decision.

### Event-driven architecture from day one

Deferred. Asynchronous job semantics are required, but the event broker and event topology will be selected when throughput, delivery, and operational needs are known.

## Evolution Criteria

A module may become a separate service when at least one is demonstrated:

- materially different scaling or hardware needs;
- independent availability or failure-isolation requirement;
- separate team ownership and release cadence;
- security or data-isolation boundary;
- sustained performance bottleneck not solved within the service;
- provider licensing/deployment constraint.

Extraction requires a new ADR covering data ownership, interface, migration, observability, security, and rollback.

## Compliance Implication

The architecture preserves provenance and separates deterministic calculations from AI interpretation, but architecture alone does not establish regulatory compliance. Final SEBI classification, disclosures, and operating controls require qualified legal review.

## Related Documents

- [Product Requirements](PRD.md)
- [High-Level Design](HLD.md)
- [Low-Level Design](LLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [Security](SECURITY.md)
