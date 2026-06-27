# Engineering Decision Records for MarketMind AI

This document summarizes implemented architecture decisions. It complements the formal ADRs under `docs/adr`.

## ADR index

| Decision | Status | Primary benefit |
|---|---|---|
| Spring Boot backend | Implemented | Fast, production-ready Java service foundation. |
| Modular monolith | Implemented | Strong boundaries without distributed-system overhead. |
| Hexagonal Architecture | Implemented | Testable use cases and replaceable adapters. |
| PostgreSQL | Implemented | Durable relational state and transactional consistency. |
| Flyway | Implemented | Controlled schema evolution. |
| Docker Compose local infra | Implemented | Repeatable local development. |
| Qdrant vector database | Implemented | Semantic retrieval for RAG. |
| Ollama local LLM | Implemented | Local AI development without cloud dependency. |
| Source Registry | Implemented | Governed source catalog. |
| Source Validation | Implemented | Reachability and capability checks. |
| Discovery Engine | Implemented | Find official document links before ingestion. |
| Source Intelligence connectors | Implemented | Official-source-first connector selection and metadata. |
| Document pipeline | Implemented | Document download, storage, versioning, extraction. |
| Pipeline orchestration | Implemented | Autonomous multi-stage processing and observability. |
| Correlation ID | Implemented | Request-level traceability. |
| Loki/Grafana logging | Implemented locally | Searchable local logs. |

## Decision: Modular monolith before microservices

### Why

MarketMind has many bounded contexts, but the team still benefits from single-repo refactoring, local transactions, shared deployment, and simpler debugging.

### Alternatives considered

| Alternative | Why not now |
|---|---|
| Microservices per module | Too much deployment and consistency overhead for current maturity. |
| Single layered package | Would blur domain boundaries. |

### Trade-off

The modular monolith requires discipline. Package boundaries must remain clean, and future extraction points should be kept visible through ports.

## Decision: Connector abstraction for source intelligence

### Why

Generic HTML crawling is insufficient for dynamic/protected sources like NSE pages. A connector abstraction lets MarketMind support official sources with specialized behavior while preserving a uniform discovery interface.

### Alternatives considered

| Alternative | Risk |
|---|---|
| Direct crawler calls from discovery | Hard to support source-specific behavior. |
| Selenium/Playwright immediately | Heavy operational cost and not implemented. |
| Manual-only links | Poor user experience. |

### Trade-off

Connectors add abstraction, but they make source capability, trust, and diagnostics first-class concepts.

## Decision: Pipeline orchestrator as single entry point

### Why

Document processing has multiple dependent stages. A single orchestrator prevents inconsistent manual sequencing and records stage-level telemetry.

### Alternatives considered

| Alternative | Risk |
|---|---|
| Controller calls each step manually | User burden and inconsistent state. |
| Hidden async listeners only | Harder to trace and retry. |
| External workflow engine now | Too heavy for current local-first implementation. |

## Decision: Local observability stack

### Why

MarketMind has background jobs. Terminal-only logs are not enough. Loki/Promtail/Grafana make local support workflows realistic.

### Trade-off

Local observability adds containers and config. The benefit is searchable logs and better production habits.

## Decision: Keep RAG local-first

### Why

Qdrant and Ollama allow AI workflows without cloud credentials. This keeps development reproducible and safe for learning.

### Trade-off

Local models may be slower or weaker than hosted models. The architecture keeps `ChatClient`, `EmbeddingClient`, and `VectorStore` abstractions so providers can change later.

