# MarketMind AI Knowledge Graphs

These maps show how the implemented parts of MarketMind AI connect. Use them for daily revision, interview storytelling, and architecture reviews.

## Product capability graph

```mermaid
flowchart TD
    Company[Company Master] --> Portfolio[Portfolio Import]
    Portfolio --> MarketData[Market Price Refresh]
    MarketData --> Dashboard[Dashboard]

    SourceRegistry[Source Registry] --> SourceValidation[Source Validation]
    SourceValidation --> SourceIntelligence[Source Intelligence]
    SourceIntelligence --> Discovery[Discovery Engine]
    Discovery --> Pipeline[Pipeline Orchestrator]

    Pipeline --> Download[Document Download]
    Download --> Storage[PDF Storage]
    Storage --> Versioning[Document Versioning]
    Versioning --> Extraction[PDF Text Extraction]
    Extraction --> Chunking[Text Chunking]
    Chunking --> Embedding[Embeddings]
    Embedding --> Qdrant[Qdrant Vector Search]
    Qdrant --> RAG[RAG Question Answering]
    RAG --> Research[Research Assistant UI]

    Scheduler[Scheduler] --> Discovery
    Scheduler --> MarketData
    Observability[Correlation IDs + Logs + Loki] --> Scheduler
    Observability --> Pipeline
    Observability --> RAG
```

## Document intelligence graph

```mermaid
flowchart LR
    Source[Trusted source] --> Discovery[Discover official PDF URL]
    Discovery --> Dedup[Normalize and deduplicate URL]
    Dedup --> Download[Download document]
    Download --> Store[Store PDF locally]
    Store --> Version[Create document version]
    Version --> Extract[Extract PDF text]
    Extract --> Chunk[Split text into chunks]
    Chunk --> Embed[Generate embeddings]
    Embed --> Index[Index in Qdrant]
    Index --> Retrieve[Retrieve relevant chunks]
    Retrieve --> LLM[Ollama chat model]
    LLM --> Answer[Answer with citations]
```

## Hexagonal dependency graph

```mermaid
flowchart TD
    API[API adapters] --> Application[Application services]
    UI[React UI] --> API
    Application --> Domain[Domain model]
    Application --> Ports[Ports / interfaces]
    Ports --> Infra[Infrastructure adapters]
    Infra --> Postgres[(PostgreSQL)]
    Infra --> Qdrant[(Qdrant)]
    Infra --> Ollama[(Ollama)]
    Infra --> FileSystem[(Local file storage)]
    Infra --> HTTP[External HTTP sources]
```

The dependency direction is intentional: the business workflow should not depend directly on PostgreSQL, Qdrant, Ollama, HTTP clients, or UI components.

## Runtime traceability graph

```mermaid
sequenceDiagram
    participant Client
    participant Filter as CorrelationIdFilter
    participant Controller
    participant Service
    participant Adapter
    participant Logs as Logs/Loki

    Client->>Filter: HTTP request + optional X-Correlation-Id
    Filter->>Filter: create/reuse correlationId
    Filter->>Logs: request started
    Filter->>Controller: continue request
    Controller->>Service: use case call
    Service->>Adapter: persistence/external call
    Adapter-->>Service: result
    Service-->>Controller: response
    Filter->>Logs: request completed with status + duration
    Filter-->>Client: response + X-Correlation-Id
```

## Learning dependency graph

```mermaid
flowchart TD
    Java[Java 21] --> Spring[Spring Boot 3]
    Spring --> REST[REST APIs + Validation]
    REST --> Hexagonal[Clean + Hexagonal Architecture]
    Hexagonal --> PostgreSQL[PostgreSQL Repositories]
    PostgreSQL --> Flyway[Flyway Migrations]
    Hexagonal --> Pipeline[Pipeline Orchestration]
    Pipeline --> RAG[RAG]
    RAG --> Embeddings[Embeddings]
    Embeddings --> Qdrant[Qdrant]
    RAG --> Ollama[Ollama]
    Pipeline --> Observability[Logging + Correlation IDs]
    Observability --> Loki[Grafana + Loki + Promtail]
    React[React + TypeScript] --> UI[Dashboard + Operational UIs]
    UI --> REST
```

Recommended path: Java → Spring → REST → Clean Architecture → PostgreSQL/Flyway → document pipeline → RAG/vector search → observability → frontend operating views → system design.

