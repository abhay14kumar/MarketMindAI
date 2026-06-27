# Implemented Concept Catalog

This catalog maps every currently implemented MarketMind AI concept to the academy learning area and representative code. It prevents the academy from drifting into features that are not yet built.

## Backend platform

| Concept | Academy page | Representative code/config |
|---|---|---|
| Java 21 | [Java 21 Backend Engineering](../01-java/JAVA_21_BACKEND_ENGINEERING.md) | `backend/pom.xml`, `backend/src/main/java/com/marketmind` |
| Spring Boot 3 | [Spring Boot Deep Dive](../02-spring-boot/SPRING_BOOT_DEEP_DIVE.md) | `MarketMindApplication`, controllers/services |
| Maven | [Java 21 Backend Engineering](../01-java/JAVA_21_BACKEND_ENGINEERING.md) | `backend/pom.xml` |
| REST APIs | [REST API Design](../03-rest-api/REST_API_DESIGN.md) | `*Controller.java` classes |
| OpenAPI/Swagger | [REST API Design](../03-rest-api/REST_API_DESIGN.md) | `@Operation`, `@Tag`, springdoc dependency |
| Validation | [REST API Design](../03-rest-api/REST_API_DESIGN.md) | Jakarta validation annotations, `GlobalExceptionHandler` |
| Error handling | [Production Support Runbook](../21-interview-master/PRODUCTION_SUPPORT_RUNBOOK.md) | `GlobalExceptionHandler`, `ErrorCode` |
| Correlation ID | [Logging and Observability](../18-observability/LOGGING_AND_OBSERVABILITY.md) | `CorrelationIdFilter` |

## Architecture patterns

| Concept | Academy page | Representative code |
|---|---|---|
| Clean Architecture | [Clean Architecture](../08-clean-architecture/CLEAN_ARCHITECTURE.md) | `application`, `domain`, `infrastructure`, `api` packages |
| Hexagonal Architecture | [DDD and Hexagonal](../09-ddd-hexagonal/DDD_AND_HEXAGONAL.md) | ports such as `VectorStore`, `Downloader`, `SourceConnector` |
| Domain Driven Design | [DDD and Hexagonal](../09-ddd-hexagonal/DDD_AND_HEXAGONAL.md) | bounded contexts: `documents`, `discovery`, `pipeline`, `ai` |
| Repository Pattern | [Code Walkthrough](../21-interview-master/CODE_WALKTHROUGH_MARKETMIND.md) | `CompanyRepository`, `DiscoveryRepository`, `PipelineJobRepository` |
| DTO Pattern | [REST API Design](../03-rest-api/REST_API_DESIGN.md) | `dto` packages |
| Mapper Pattern | [Code Walkthrough](../21-interview-master/CODE_WALKTHROUGH_MARKETMIND.md) | `*Mapper.java` classes |

## Data and infrastructure

| Concept | Academy page | Representative code/config |
|---|---|---|
| PostgreSQL | [PostgreSQL Deep Dive](../04-postgresql/POSTGRESQL_DEEP_DIVE.md) | JDBC repositories, JPA repositories |
| Flyway | [Flyway Migrations](../05-flyway/FLYWAY_MIGRATIONS.md) | `backend/src/main/resources/db/migration` |
| Docker Compose | [Docker Local Infrastructure](../06-docker/DOCKER_LOCAL_INFRA.md) | `docker-compose.yml` |
| Redis | [Docker Local Infrastructure](../06-docker/DOCKER_LOCAL_INFRA.md) | local infrastructure service; not yet a deep business dependency |
| Qdrant | [Qdrant Vector Database](../16-qdrant/QDRANT_VECTOR_DATABASE.md) | `QdrantVectorStore` |
| Ollama | [Ollama and Local LLM](../17-ollama-llm/OLLAMA_AND_LOCAL_LLM.md) | `OllamaClient` |
| Grafana/Loki/Promtail | [Logging and Observability](../18-observability/LOGGING_AND_OBSERVABILITY.md) | `monitoring/`, `docker-compose.yml` |
| Helm/Kubernetes readiness | [Kubernetes and Helm Readiness](../19-kubernetes-helm/KUBERNETES_HELM_READINESS.md) | `helm/marketmind-ai` |

## Source and document intelligence

| Concept | Academy page | Representative code |
|---|---|---|
| Source Registry | [Source Registry Module](../10-source-registry/SOURCE_REGISTRY_MODULE.md) | `SourceRegistryService`, `JdbcSourceRegistryRepository` |
| Source Validation | [Source Registry Module](../10-source-registry/SOURCE_REGISTRY_MODULE.md) | `SourceValidationService` |
| Source Intelligence | [System Design Decision Guide](../20-system-design/SYSTEM_DESIGN_DECISION_GUIDE.md) | `SourceIntelligenceService`, `SourceConnectorFactory` |
| Connector architecture | [Engineering Decisions](../20-system-design/ENGINEERING_DECISIONS.md) | `SourceConnector`, NSE/BSE/SEBI/RBI connectors |
| Discovery Engine | [System Design Decision Guide](../20-system-design/SYSTEM_DESIGN_DECISION_GUIDE.md) | `DiscoveryService` |
| Generic HTML PDF Crawler | [Production Support Runbook](../21-interview-master/PRODUCTION_SUPPORT_RUNBOOK.md) | `GenericHtmlPdfCrawler` |
| Document Download | [Document Pipeline Module](../11-document-pipeline/DOCUMENT_PIPELINE_MODULE.md) | `DocumentDownloadService` |
| PDF Storage | [Document Pipeline Module](../11-document-pipeline/DOCUMENT_PIPELINE_MODULE.md) | `LocalFileStorageProvider` |
| Document Versioning | [Document Pipeline Module](../11-document-pipeline/DOCUMENT_PIPELINE_MODULE.md) | `DefaultVersionManager` |
| PDF Text Extraction | [Document Pipeline Module](../11-document-pipeline/DOCUMENT_PIPELINE_MODULE.md) | `PdfTextExtractionService`, `PdfBoxDocumentParser` |

## AI and RAG

| Concept | Academy page | Representative code |
|---|---|---|
| Chunking | [Embeddings Deep Dive](../15-embeddings/EMBEDDINGS_DEEP_DIVE.md) | `TextChunkingService` |
| Embedding | [Embeddings Deep Dive](../15-embeddings/EMBEDDINGS_DEEP_DIVE.md) | `DocumentEmbeddingService`, `EmbeddingClient` |
| Vector Search | [Qdrant Vector Database](../16-qdrant/QDRANT_VECTOR_DATABASE.md) | `VectorStore`, `QdrantVectorStore` |
| RAG | [RAG Deep Dive](../14-rag/RAG_DEEP_DIVE.md) | `RagQuestionAnswerService` |
| AI Question Answering | [RAG Deep Dive](../14-rag/RAG_DEEP_DIVE.md) | `AiController`, `RagQuestionAnswerService` |

## Portfolio, market data, scheduler, and UI

| Concept | Academy page | Representative code |
|---|---|---|
| Portfolio Import | [Portfolio Intelligence](../12-portfolio-intelligence/PORTFOLIO_INTELLIGENCE_MODULE.md) | `PortfolioService`, `ZerodhaHoldingsXlsxParser` |
| Real Market Price Refresh | [Price Feed Module](../13-market-data/PRICE_FEED_MODULE.md) | `RealPriceRefreshService`, `PublicQuotePriceProvider` |
| Scheduler | [Production Support Runbook](../21-interview-master/PRODUCTION_SUPPORT_RUNBOOK.md) | `SchedulerService`, `SchedulerController` |
| Pipeline Orchestration | [System Design Decision Guide](../20-system-design/SYSTEM_DESIGN_DECISION_GUIDE.md) | `PipelineOrchestrator` |
| Pipeline Stages | [Code Walkthrough](../21-interview-master/CODE_WALKTHROUGH_MARKETMIND.md) | `PipelineStage`, `PipelineEvent`, `PipelineJob` |
| Dashboard | [React + TypeScript Frontend](../07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md) | `DashboardPage.tsx` |
| Source Intelligence UI | [React + TypeScript Frontend](../07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md) | `SourcesPage.tsx` |
| Discovery UI | [React + TypeScript Frontend](../07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md) | `DiscoveryPage.tsx` |
| Pipeline UI | [React + TypeScript Frontend](../07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md) | `PipelineMonitorPage.tsx` |
| Notification Center | [React + TypeScript Frontend](../07-react-typescript/REACT_TYPESCRIPT_FRONTEND.md) | `NotificationProvider.tsx`, `AppShell.tsx` |

## Not-currently-implemented boundary

The academy should not teach these as implemented behavior yet:

- full NSE-specific scraper with protected/session API handling;
- Selenium/Playwright crawling;
- production cloud monitoring;
- production Kubernetes deployment;
- OCR for scanned PDFs;
- authentication/RBAC;
- circuit breakers;
- distributed queue-backed pipeline execution.

