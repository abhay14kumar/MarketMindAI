# MEKS Concept Graph

```mermaid
flowchart TB
    CORRECT[Correctness] --> TEST[Testing]
    CORRECT --> DATA[Data Integrity]
    TRUST[Trust] --> SEC[Security]
    TRUST --> PROV[Provenance]
    TRUST --> UX[Transparent UX]
    REL[Reliability] --> FAIL[Failure Design]
    REL --> OBS[Observability]
    REL --> REC[Recovery]
    SCALE[Scalability] --> CAP[Capacity]
    SCALE --> ASYNC[Asynchrony]
    SCALE --> CACHE[Caching]
    CHANGE[Evolvability] --> BOUND[Boundaries]
    CHANGE --> CONTRACT[Contracts]
    CHANGE --> MIG[Migration]
    JUDGE[Engineering Judgment] --> CORRECT
    JUDGE --> TRUST
    JUDGE --> REL
    JUDGE --> SCALE
    JUDGE --> CHANGE
```

Use this graph to connect technologies to quality attributes. For example,
Qdrant is not learned in isolation; it participates in provenance, retrieval
quality, scalability, failure design, and operations.
