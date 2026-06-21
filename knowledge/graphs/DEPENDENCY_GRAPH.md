# MEKS Dependency Graph

```mermaid
flowchart LR
    LANG[Java] --> JVM[JVM]
    JVM --> CONC[Concurrency]
    LANG --> SB[Spring Boot]
    HTTP[HTTP] --> REST[REST]
    REST --> SB
    DB[Database Fundamentals] --> PG[PostgreSQL]
    PG --> FLY[Schema Migration]
    SB --> HEX[Hexagonal Architecture]
    PG --> HEX
    LINUX[Linux and Networking] --> DOCKER[Docker]
    DOCKER --> K8S[Kubernetes]
    K8S --> HELM[Helm]
    SB --> OBS[Observability]
    K8S --> OBS
    TEST[Testing] --> REL[Reliability]
    OBS --> REL
    EMB[Embeddings] --> QD[Qdrant]
    QD --> RAG[RAG]
    LLM[LLMs] --> RAG
    RAG --> AGENTS[AI Agents]
```

## Dependency Policy

- A prerequisite may be satisfied through demonstrated experience.
- Topic manifests should link only meaningful dependencies.
- Circular relationships are allowed in advanced learning but should not hide
  the first useful entry point.
