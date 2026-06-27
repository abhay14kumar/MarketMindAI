# MarketMind AI Code Walkthrough

This guide explains important classes by responsibility, design pattern, reason for existence, alternatives, and future improvements.

## How to read the codebase

Start with a vertical slice, not a random package.

Recommended slices:

1. Company master: simplest hexagonal example.
2. Source registry and validation.
3. Discovery and source intelligence.
4. Document download and extraction.
5. Pipeline orchestration.
6. Embeddings and RAG.
7. Portfolio import and price refresh.
8. Observability and error handling.

## Company module

| Class | Responsibility | Pattern | Why created | Alternative | Future improvement |
|---|---|---|---|---|---|
| `Company` | Domain representation of a company | Domain model | Keeps company concepts outside persistence | Use JPA entity directly | Add richer invariants |
| `CompanyService` | Company use cases | Application service | Keeps controller thin | Controller logic | Add caching/search |
| `CompanyRepository` | Persistence contract | Repository port | Decouples service from DB | Direct Spring Data use | Add query specifications |
| `CompanyPersistenceAdapter` | Implements repository with persistence | Adapter | Converts domain to JPA | Active Record | Optimize page queries |
| `CompanyController` | HTTP API | Inbound adapter | Exposes REST contract | Service as controller | Add more OpenAPI examples |

## Source Registry and Validation

| Class | Responsibility | Pattern |
|---|---|---|
| `SourceRegistry` | Registered source aggregate-like domain object | Domain model |
| `SourceRegistryService` | Create/list/update/validate source use cases | Application service |
| `SourceValidationService` | Reachability, robots, PDF capability checks | Application service |
| `ReachabilityChecker` | Port for HTTP reachability | Port |
| `RobotsTxtChecker` | Port for robots checks | Port |
| `PdfCapabilityChecker` | Port for PDF capability | Port |
| `JdbcSourceRegistryRepository` | PostgreSQL persistence | Adapter |
| `HttpReachabilityClient` | HTTP reachability implementation | Adapter |

Why created: source trust begins before discovery. MarketMind needs to know which sources exist, whether they are reachable, and what capabilities they expose.

## Source Intelligence

| Class | Responsibility | Pattern |
|---|---|---|
| `SourceConnector` | Unified connector contract | Port/strategy |
| `SourceConnectorFactory` | Selects best connector, official first | Factory/strategy |
| `SourceIntelligenceService` | Catalog, metrics, validation, refresh, activity | Application service |
| `SourceCapabilityDetector` | Infers supported formats from connector and URL | Domain/application utility |
| `NseSourceConnector`, `BseSourceConnector`, `SebiSourceConnector`, `RbiSourceConnector` | Official connector implementations | Adapter/strategy |
| `GenericRestSourceConnector`, `RssSourceConnector`, `CompanyIrSourceConnector` | General connector implementations | Adapter/strategy |
| `JdbcSourceIntelligenceRepository` | Stores profiles/activity/coverage | Adapter |

Design reason: direct crawler usage does not scale to enterprise-grade source behavior. Connectors make source trust, capability, and source-specific behavior explicit.

Future improvements: source-specific APIs, authentication support, rate-limit policies, source SLAs, event streaming.

## Discovery

| Class | Responsibility |
|---|---|
| `DiscoveryService` | Runs discovery, deduplicates URLs, stores discovered metadata. |
| `DiscoveryRepository` | Persistence port for jobs/documents/source runs. |
| `DefaultDiscoveryDeduplicationService` | URL normalization and duplicate detection. |
| `KeywordDiscoveryClassificationService` | Classifies document type from title/URL. |
| `GenericHtmlPdfCrawler` | Fetches HTML and extracts direct PDF links with diagnostics. |
| `TestStaticCrawler` | Deterministic local test source. |

Key design point: discovery should not automatically imply ingestion. It stores metadata and diagnostics first.

## Documents

| Class | Responsibility |
|---|---|
| `DocumentDownloadService` | Validates command, downloads PDF, stores file, creates document/version/job. |
| `HttpDocumentDownloader` | Fetches remote document bytes. |
| `LocalFileStorageProvider` | Stores downloaded PDF locally. |
| `DefaultVersionManager` | Handles version creation semantics. |
| `PdfTextExtractionService` | Extracts text and stores extraction result. |
| `PdfBoxDocumentParser` | PDFBox-based parser adapter. |
| `DocumentPersistenceAdapter` | Document persistence operations. |

Production concern: downloaded files are untrusted input. Keep size limits, content-type checks, safe storage paths, and parsing failure handling.

## Pipeline

| Class | Responsibility |
|---|---|
| `PipelineOrchestrator` | Single orchestration entry point for autonomous processing. |
| `PipelineJobRepository` | Port for pipeline job/stage/event persistence. |
| `JdbcPipelineJobRepository` | PostgreSQL adapter for orchestration state. |
| `DiscoveryPipelineListener` | Starts pipeline from newly discovered documents. |
| `AutomatedDocumentProcessingTrigger` | Bridges completed downloads to document pipeline behavior. |
| `PipelineOrchestrationController` | REST APIs for start/retry/list/detail/events/metrics. |

Pattern: workflow orchestration with stage state machine and retry policy.

Future improvements: distributed locks, queue-backed execution, idempotency keys, circuit breakers, dead-letter queue.

## AI and RAG

| Class | Responsibility |
|---|---|
| `DocumentEmbeddingService` | Creates chunks, embeddings, jobs, and vector entries. |
| `TextChunkingService` | Splits extracted text into chunks. |
| `EmbeddingClient` | Embedding provider port. |
| `ChatClient` | Chat/LLM provider port. |
| `QdrantVectorStore` | Vector indexing/search adapter. |
| `OllamaClient` | Local LLM/embedding adapter. |
| `RagQuestionAnswerService` | Retrieves chunks and generates grounded answers. |
| `JdbcRagRepository` | Stores chunks, embedding jobs, answers. |

Key design point: AI provider details are infrastructure. RAG workflow belongs in the application layer.

## Portfolio and Market Data

| Class | Responsibility |
|---|---|
| `ZerodhaHoldingsXlsxParser` | Parses uploaded Zerodha holdings workbook. |
| `PortfolioService` | Imports holdings and computes summaries. |
| `JdbcPortfolioRepository` | Portfolio persistence. |
| `PriceFeedService` | Manual/mock price feed operations. |
| `RealPriceRefreshService` | Real provider refresh workflow. |
| `PublicQuotePriceProvider` | Public quote API adapter. |
| `SymbolMapper` | Maps symbols for providers/exchanges. |

Production concern: price providers fail, rate-limit, and return partial data. Keep status visible.

## Scheduler

| Class | Responsibility |
|---|---|
| `SchedulerService` | Job registry, run history, run-now behavior. |
| `SchedulerJobExecutor` | Execution abstraction. |
| `DiscoverySchedulerJobExecutor` | Connects scheduler to discovery runs. |
| `MockSchedulerRepository` | Current scheduler persistence/mock implementation. |

Design point: UI must distinguish seeded/mock/manual/real behavior.

## Observability and errors

| Class | Responsibility |
|---|---|
| `CorrelationIdFilter` | Adds/reuses correlation ID, logs request start/completion. |
| `GlobalExceptionHandler` | Converts validation/domain/external errors to consistent API errors. |
| `ErrorCode` | Stable machine-readable error taxonomy. |
| `FieldViolation` | Field-level validation details. |

Code review checklist:

- Is the error actionable?
- Is the correlation ID present?
- Are sensitive details hidden?
- Is the log searchable?

