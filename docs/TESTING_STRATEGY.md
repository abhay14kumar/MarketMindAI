# MarketMind AI — Testing Strategy

**Status:** Draft for Phase 1

**Objective:** Verify software correctness, financial-data integrity, AI grounding, security, and operational resilience.

## 1. Principles

- Test behavior at the lowest useful level and critical journeys end to end.
- Keep tests deterministic, isolated, readable, and repeatable.
- Financial calculations require exact reviewed fixtures.
- AI tests evaluate evidence use, citations, uncertainty, and safety rather than exact wording.
- External providers and models are isolated behind adapters.
- No test depends on production credentials or sensitive production data.
- Release gates become stricter as features move from retrieval to decision-support labels.

## 2. Test Layers

### 2.1 Static checks

- Formatting and linting
- Type checking and compilation
- API/schema validation
- Database migration linting where available
- Static application security testing
- Secret scanning
- Dependency, license, and container scanning

### 2.2 Unit tests

Cover pure logic:

- money, dates, periods, units, and currency rules;
- portfolio holdings and cost basis;
- financial ratios and valuation scenarios;
- authorization policies;
- state machines and retry classification;
- chunking and metadata extraction;
- citation mapping and policy decisions.

Unit tests do not require network services.

### 2.3 Component/service tests

Test a service with controlled dependencies:

- Spring MVC/API validation and error mapping;
- persistence repositories against PostgreSQL;
- FastAPI routes and Pydantic schemas;
- Qdrant indexing/retrieval adapters;
- Ollama adapter behavior using stubs for routine tests;
- provider parsing with captured licensed/synthetic fixtures.

### 2.4 Contract tests

- OpenAPI request/response compatibility
- Backend-to-AI-service contracts
- Provider adapter contracts
- Error-code and job-state compatibility
- Agent evidence and final-report JSON schemas

Consumer/provider contract checks should block incompatible changes.

### 2.5 Integration tests

Run with real disposable dependencies, preferably containers:

- PostgreSQL migrations and constraints
- Qdrant collection and filters
- Backend/AI service authentication
- Ollama smoke tests in a dedicated optional profile
- Ingestion through parse, chunk, embed, and index
- Research retrieval through citation validation

### 2.6 End-to-end tests

Critical journeys:

- authenticate and search a company;
- ingest or inspect an available document;
- ask a filing question and open its citation;
- create a portfolio and record transactions;
- view calculated allocation and concentration;
- request a portfolio-aware research report;
- observe safe failure when evidence/model/provider is unavailable.

E2E suites remain small and high value.

## 3. Frontend Testing

- Unit-test formatting and state transformations.
- Component-test accessible labels, keyboard flows, errors, stale-data notices, citations, and disclaimers.
- Mock the network at the API boundary.
- E2E-test supported browsers and responsive breakpoints.
- Add automated accessibility checks, backed by manual keyboard/screen-reader review for critical journeys.
- Avoid brittle snapshots of large rendered trees.

## 4. Backend Testing

- Unit-test domain and calculation logic without Spring where possible.
- Use slice tests for controllers/repositories where useful.
- Use PostgreSQL, not an incompatible in-memory substitute, for database integration.
- Test transaction boundaries, optimistic locking, idempotency, pagination, and authorization.
- Verify public problem-details responses and internal logging redaction.
- Test provider timeouts, correction records, stale data, and partial availability.

## 5. AI Service and RAG Testing

### 5.1 Parsing and chunking

Fixtures include:

- digital and scanned PDFs;
- multi-column layouts;
- tables with units and footnotes;
- annual and quarterly period boundaries;
- amended/corrected documents;
- malformed and malicious documents.

Measure text, table, page/section, and metadata accuracy.

### 5.2 Retrieval

For each evaluation question, maintain:

- expected company/document/period;
- relevant chunk or source set;
- forbidden/irrelevant sources;
- whether abstention is expected.

Track recall@k, precision@k, ranking metrics, filter accuracy, and retrieval latency.

### 5.3 Grounding and citations

Evaluate:

- factual correctness;
- entailment between claim and citation;
- citation precision and material-claim coverage;
- source authority and freshness;
- faithful representation of conflicting evidence;
- unsupported-claim rate;
- correct abstention.

### 5.4 Agent testing

Each agent is tested independently for:

- schema compliance;
- use of only allowed evidence/tools;
- role boundaries;
- calculations/assumptions;
- missing-information reporting;
- counter-evidence and confidence.

The CIO Agent is tested for:

- preserving specialist disagreement;
- not introducing new facts;
- omitting labels below policy thresholds;
- inclusion of risks, sources, freshness, assumptions, and disclaimer.

### 5.5 Non-determinism

- Pin model/configuration versions for evaluation.
- Use bounded generation parameters.
- Repeat a representative subset to measure variance.
- Assert structured properties and evidence behavior, not exact prose.
- Record model, prompt, retrieval, dataset, and policy versions with results.

## 6. Financial Calculation Testing

Use independently reviewed golden datasets for:

- buys, sells, fees, dividends, splits, and corrections;
- average/FIFO cost-basis method selected by product policy;
- realized and unrealized gain/loss;
- multiple currencies and missing FX;
- allocation and concentration;
- zero/negative values and divide-by-zero;
- fiscal period alignment;
- common financial ratios;
- valuation sensitivity scenarios.

Properties/invariants:

- allocations reconcile to the documented total within explicit rounding tolerance;
- money never uses binary floating-point arithmetic;
- equivalent transaction permutations behave according to the selected method;
- calculations retain source, timestamp, formula version, and input snapshot.

## 7. Security Testing

- Authentication and session tests
- Horizontal and vertical authorization tests
- Tenant-isolation tests across PostgreSQL, Qdrant filters, caches, jobs, and citations
- Input injection and output encoding
- File upload, parser sandbox, archive bomb, and malformed-file tests
- SSRF and redirect tests for source acquisition
- Secret/log leakage tests
- Dependency/container/configuration scanning
- Rate-limit and resource-exhaustion tests
- Prompt-injection, tool abuse, indirect injection, and data-exfiltration tests

An independent security assessment is required before controlled beta.

## 8. Performance and Resilience

### 8.1 Performance

Establish baselines for:

- company/portfolio API latency;
- ingestion throughput by document size/type;
- Qdrant retrieval latency;
- embedding throughput;
- model and end-to-end research latency;
- concurrent job capacity.

Targets are defined after representative workloads are measured.

### 8.2 Resilience

Test:

- provider timeout and rate limiting;
- PostgreSQL/Qdrant/Ollama restarts;
- duplicate job delivery;
- worker interruption and lease recovery;
- partial vector upsert;
- stale cache/data;
- retry exhaustion and dead-letter handling;
- backup restore and index rebuild.

The expected result is transparent degradation, not fabricated completeness.

## 9. Test Data

- Use synthetic companies/portfolios for most tests.
- Use redistributable public filing excerpts only when licensing permits.
- Record source and license for evaluation artifacts.
- Remove or anonymize personal data.
- Never commit credentials, production exports, or private user documents.
- Version golden datasets and require review for expected-answer changes.

## 10. Environments

| Environment | Purpose |
| --- | --- |
| Local | Fast unit/component feedback and optional Compose integration |
| CI | Reproducible checks with disposable PostgreSQL/Qdrant and stubbed model by default |
| AI evaluation | Controlled Ollama/model runs against versioned datasets |
| Staging | End-to-end, performance, resilience, and security validation |
| Production | Synthetic probes and monitoring; no destructive tests |

## 11. CI Quality Gates

Initial gates once code exists:

- build/compile succeeds;
- formatting, linting, and type checks pass;
- unit and contract tests pass;
- migrations apply from empty and supported prior state;
- critical/high secret, dependency, and code findings are resolved according to policy;
- changed AI components pass relevant evaluation suites;
- OpenAPI compatibility passes;
- coverage does not regress below agreed risk-based thresholds.

Coverage is a signal, not the goal. High-risk calculation, authorization, citation, and policy code requires near-complete behavioral coverage.

## 12. AI Release Gates

Model, prompt, parser, embedding, chunking, retrieval, or agent changes require:

- reproducible evaluation report;
- no material regression in retrieval recall or citation correctness;
- no increase beyond threshold in unsupported claims;
- prompt-injection and tenant-isolation pass;
- acceptable abstention behavior;
- reviewed changes to decision-support label distribution;
- rollback configuration and index compatibility.

Exact numerical thresholds are set after the Phase 3 baseline.

## 13. Defect Severity

| Severity | Examples |
| --- | --- |
| Critical | Cross-user data exposure, secret exposure, trade execution, materially fabricated cited evidence |
| High | Incorrect portfolio totals, broken authorization, systematic unsupported financial claims |
| Medium | Limited incorrect analysis, stale-data label omission, degraded workflow with workaround |
| Low | Minor UX, wording, or non-critical observability issue |

Critical and high defects block release unless an explicitly approved risk process says otherwise.

## 14. Test Ownership and Reporting

- Feature teams own unit, component, and contract tests.
- QA coordinates end-to-end, exploratory, accessibility, and release evidence.
- AI/ML owners maintain evaluation datasets and metric pipelines.
- Security owns/advises threat-driven test requirements.
- Financial-domain reviewers approve critical calculation and research fixtures.

Release reports include software results, AI evaluation versions, known limitations, unresolved risks, and rollback readiness.

## 15. Related Documents

- [Product Requirements](PRD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [Security](SECURITY.md)
- [Coding Standards](CODING_STANDARDS.md)
