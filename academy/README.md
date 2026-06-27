# MarketMind AI Academy

The academy is the learning platform for MarketMind AI. It teaches the implemented system from beginner level to Principal Engineer depth: backend engineering, AI/RAG, source intelligence, document processing, portfolio intelligence, production support, system design, and interview preparation.

The rule is simple: teach only what exists in the repository today, and teach it through real MarketMind code, flows, failures, and trade-offs.

## How to use the academy

| Goal | Start here |
|---|---|
| Understand the product | [MarketMind AI Overview](00-start-here/01_MARKETMIND_AI_OVERVIEW.md) |
| Build a learning path | [Skills Map](00-start-here/02_SKILLS_MAP.md) |
| Write future academy pages | [Academy Content Standard](00-start-here/03_ACADEMY_CONTENT_STANDARD.md) |
| Revise architecture quickly | [Knowledge Graphs](00-start-here/04_KNOWLEDGE_GRAPHS.md) |
| Find where each implemented concept is taught | [Implemented Concept Catalog](00-start-here/05_IMPLEMENTED_CONCEPT_CATALOG.md) |
| Become stronger as an engineer | [World-Class Engineer Guide](01-engineering-foundation/01_HOW_TO_BECOME_A_WORLD_CLASS_ENGINEER.md) |
| Prepare for interviews | [Complete Interview Handbook](21-interview-master/COMPLETE_INTERVIEW_HANDBOOK.md) |
| Debug production issues | [Production Support Runbook](21-interview-master/PRODUCTION_SUPPORT_RUNBOOK.md) |
| Review code by module | [Code Walkthrough](21-interview-master/CODE_WALKTHROUGH_MARKETMIND.md) |
| Practice daily | [Assignments and Labs](21-interview-master/ASSIGNMENTS_AND_LABS.md) |

## Learning tracks

### Backend foundation

1. [Java 21 Backend Engineering](01-java/JAVA_21_BACKEND_ENGINEERING.md)
2. [Spring Boot Deep Dive](02-spring-boot/SPRING_BOOT_DEEP_DIVE.md)
3. [REST API Design](03-rest-api/REST_API_DESIGN.md)
4. [PostgreSQL Deep Dive](04-postgresql/POSTGRESQL_DEEP_DIVE.md)
5. [Flyway Migrations](05-flyway/FLYWAY_MIGRATIONS.md)
6. [Docker Local Infrastructure](06-docker/DOCKER_LOCAL_INFRA.md)

### Frontend and product UX

1. [React + TypeScript Frontend Engineering](07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md)
2. Dashboard, discovery, source intelligence, pipeline, scheduler, portfolio, and research assistant pages in `frontend/src/pages`.

### Architecture

1. [Clean Architecture](08-clean-architecture/CLEAN_ARCHITECTURE.md)
2. [DDD and Hexagonal Architecture](09-ddd-hexagonal/DDD_AND_HEXAGONAL.md)
3. [System Design](20-system-design/MARKETMIND_SYSTEM_DESIGN.md)
4. [System Design Decision Guide](20-system-design/SYSTEM_DESIGN_DECISION_GUIDE.md)
5. [Engineering Decisions](20-system-design/ENGINEERING_DECISIONS.md)

### MarketMind modules

1. [Source Registry](10-source-registry/SOURCE_REGISTRY_MODULE.md)
2. [Document Pipeline](11-document-pipeline/DOCUMENT_PIPELINE_MODULE.md)
3. [Portfolio Intelligence](12-portfolio-intelligence/PORTFOLIO_INTELLIGENCE_MODULE.md)
4. [Market Data and Price Feed](13-market-data/PRICE_FEED_MODULE.md)
5. Source Intelligence implementation under `backend/src/main/java/com/marketmind/sourceintelligence`.
6. Discovery implementation under `backend/src/main/java/com/marketmind/discovery`.
7. Pipeline orchestration implementation under `backend/src/main/java/com/marketmind/pipeline`.

### AI and retrieval

1. [RAG Deep Dive](14-rag/RAG_DEEP_DIVE.md)
2. [Embeddings Deep Dive](15-embeddings/EMBEDDINGS_DEEP_DIVE.md)
3. [Qdrant Vector Database](16-qdrant/QDRANT_VECTOR_DATABASE.md)
4. [Ollama and Local LLM](17-ollama-llm/OLLAMA_AND_LOCAL_LLM.md)

### Operations

1. [Logging and Observability](18-observability/LOGGING_AND_OBSERVABILITY.md)
2. [Kubernetes and Helm Readiness](19-kubernetes-helm/KUBERNETES_HELM_READINESS.md)
3. [Production Support Runbook](21-interview-master/PRODUCTION_SUPPORT_RUNBOOK.md)

### Interview and revision

1. [Project Interview Guide](21-interview-master/PROJECT_INTERVIEW_GUIDE.md)
2. [Complete Interview Handbook](21-interview-master/COMPLETE_INTERVIEW_HANDBOOK.md)
3. [Follow-up Questions](21-interview-master/FOLLOW_UP_QUESTIONS.md)
4. [Cheat Sheets](21-interview-master/CHEATSHEETS.md)
5. [Flashcards](21-interview-master/FLASHCARDS.md)
6. [Case Studies](21-interview-master/CASE_STUDIES.md)

## Academy rules

- Teach through MarketMind examples and production constraints.
- Do not document unimplemented features as current behavior.
- Use tables, Mermaid diagrams, examples, failure scenarios, and code references.
- Support progression from beginner to Principal Engineer.
- Include interview questions, hands-on exercises, assignments, and operational guidance.
- Improve existing documents rather than creating duplicate pages for the same concept.

Use the [Learning Graph](../knowledge/graphs/LEARNING_GRAPH.md) and [Academy Knowledge Graphs](00-start-here/04_KNOWLEDGE_GRAPHS.md) to sequence study.
