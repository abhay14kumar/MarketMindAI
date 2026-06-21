# MEKS Knowledge Graph

```mermaid
flowchart TB
    EF[Engineering Foundation] --> JAVA[Java and JVM]
    EF --> ARCH[Architecture and DDD]
    JAVA --> SPRING[Spring and Spring Boot]
    NET[Networking and HTTP] --> API[REST, GraphQL, Identity]
    SPRING --> API
    DATA[Database and PostgreSQL] --> DIST[Distributed Systems]
    API --> DIST
    DIST --> PROD[Production Engineering]
    PLATFORM[Linux, Docker, Kubernetes, Cloud] --> PROD
    TEST[Testing and Security] --> PROD

    FIN[Finance Domain] --> MM[MarketMind Modules]
    SPRING --> MM
    DATA --> MM
    DIST --> MM

    ML[ML and Deep Learning] --> LLM[Transformers and LLMs]
    LLM --> EMB[Embeddings and Vector Databases]
    EMB --> RAG[RAG]
    RAG --> AGENT[AI Agents and Evaluation]
    MM --> RAG

    PROD --> LEAD[Leadership and Architecture]
    MM --> LEAD
    LEAD --> CAREER[Staff, Principal, Architect, Manager]
```

## Reading the Graph

Arrows express strong learning dependencies, not mandatory bureaucracy.
Experienced engineers may enter at any node, but should verify upstream mental
models before claiming production mastery.
