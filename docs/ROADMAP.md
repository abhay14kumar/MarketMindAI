# MarketMind AI — Development Roadmap

**Status:** Draft

**Planning model:** Outcome-based phases; dates are assigned only after capacity and dependencies are known.

## 1. Roadmap Principles

- Build trust, provenance, and security before recommendation-like experiences.
- Deliver a narrow end-to-end research path before expanding breadth.
- Use deterministic calculations for financial and portfolio metrics.
- Gate AI changes with retrieval, grounding, citation, and safety evaluations.
- Keep all outputs as decision support; do not implement trading or automatic rebalancing.
- Promote capabilities only when data rights and compliance requirements are understood.

## 2. Phase 1 — Product and Architecture

**Objective:** Establish an implementation-ready product, architecture, AI, security, and quality baseline.

### Deliverables

- Product requirements, personas, scope, journeys, and non-goals
- High-level and low-level system design
- AI/RAG ingestion, retrieval, citation, and evaluation design
- Multi-agent roles: Filing, Financial, Valuation, Risk, Portfolio, and CIO
- API guidelines and coding standards
- Security and threat-model baseline
- Testing strategy and release gates
- Architecture-style ADR
- Initial provider, licensing, identity, and deployment decision backlog

### Exit criteria

- Documents have consistent terminology and service ownership.
- Decision-support and SEBI disclaimers are defined for legal review.
- No application code or secrets have been added.
- Phase 2 implementation backlog can be estimated.

## 3. Phase 2 — Platform Foundation

**Objective:** Create the secure, testable local platform skeleton.

### Deliverables

- React + TypeScript + Vite frontend scaffold
- Spring Boot 3 + Java 21 + Maven backend scaffold
- Python FastAPI AI-service scaffold
- Docker Compose for PostgreSQL, Qdrant, Ollama, and services
- Database migrations and seed strategy
- Service health/readiness checks and configuration validation
- Authentication integration spike and authorization model
- Structured logging, correlation IDs, metrics, and tracing baseline
- CI for formatting, linting, type checks, tests, dependency and secret scanning
- Sanitized `.env.example`; runtime secrets remain outside source control

### Exit criteria

- A contributor can start the stack from documented commands.
- Services communicate over private interfaces with health checks.
- CI blocks critical security and quality failures.
- No public AI-service, database, Qdrant, or Ollama exposure.

## 4. Phase 3 — Document Intelligence and Grounded Research MVP

**Objective:** Answer scoped filing questions with verified citations.

### Deliverables

- Company/instrument master and provider mappings
- Approved-source registry and document metadata
- Annual report, quarterly result, filing, and transcript ingestion
- Parsing/OCR abstraction, checksums, deduplication, and versioning
- Section-aware chunking and `nomic-embed-text` indexing in Qdrant
- Company/document/date-filtered retrieval
- Single research workflow using `llama3.1`
- Page/section citations, citation validation, and abstention
- Research job status, saved reports, and user feedback
- Initial golden evaluation dataset and dashboards

### Exit criteria

- Supported questions meet agreed retrieval and citation thresholds.
- Unsupported questions abstain reliably.
- Prompt-injection and source-authorization tests pass.
- Users can navigate from an answer to its source location.

## 5. Phase 4 — Financial Analysis and Multi-Agent Research

**Objective:** Add repeatable financial analysis and bounded specialist collaboration.

### Deliverables

- Normalized financial observations and period comparisons
- Versioned deterministic ratio and financial-calculation engine
- Filing Analyst and Financial Analyst
- Valuation Analyst with explicit scenario assumptions
- Risk Analyst with counter-thesis requirements
- CIO Agent structured synthesis
- Agent evidence packets, schema validation, and dissent preservation
- Expanded evaluations for tables, units, amended filings, valuation, and contradictions
- Feature-flagged buy/watch/hold/rotate review labels

### Exit criteria

- Calculations reconcile against reviewed fixtures.
- Agent output is grounded in supplied evidence/calculations.
- CIO synthesis preserves material disagreement and uncertainty.
- Decision-support labels pass compliance, safety, and quality review.

## 6. Phase 5 — Portfolio Intelligence

**Objective:** Connect research to user-controlled portfolio context.

### Deliverables

- Portfolios, transactions, holdings, watchlists, and base-currency support
- Deterministic cost basis, P&L, allocation, and concentration
- Portfolio Agent and portfolio-aware Risk Agent
- Hypothetical scenario analysis with explicit assumptions
- Source- and timestamp-aware monitoring alerts
- Portfolio review workspace and scheduled summaries
- Privacy controls, data export, and deletion workflows

### Exit criteria

- Tenant isolation and authorization tests pass.
- Portfolio calculations reconcile with independent fixtures.
- Portfolio output never causes an automatic transaction.
- Alerts identify trigger, source, freshness, and limitations.

## 7. Phase 6 — Production Readiness and Controlled Beta

**Objective:** Validate the service under realistic operational, security, and compliance conditions.

### Deliverables

- External penetration test and threat-model review
- Data-protection, retention, backup, restore, and incident-response procedures
- Provider licensing and legal/compliance sign-off
- Rate limits, abuse controls, audit trails, and operational runbooks
- Load, resilience, disaster-recovery, accessibility, and browser testing
- Model/prompt/index rollout and rollback procedures
- Cost and capacity model
- Controlled beta with monitored cohorts and feedback adjudication

### Exit criteria

- Security and compliance launch blockers are closed.
- Restore and rollback exercises succeed.
- Reliability and AI quality objectives are met.
- Known limitations are documented for users and operators.

## 8. Phase 7 — Expansion

Potential capabilities, subject to evidence and user need:

- Additional exchanges, geographies, currencies, and asset classes
- Broader peer and industry analysis
- Enhanced hybrid retrieval and reranking
- Scenario and stress-testing tools
- Mobile-responsive and notification improvements
- Carefully governed integrations with external portfolio systems

Brokerage execution and autonomous trading remain excluded unless a separate product, legal, security, and architecture decision explicitly authorizes them.

## 9. Cross-Cutting Workstreams

### Data and licensing

- Select approved providers.
- Record lineage, freshness, correction, and permitted-use policy.
- Build reconciliation and provider-failure handling.

### AI quality

- Expand golden datasets continuously.
- Track retrieval, grounding, citation, abstention, and safety metrics.
- Require evaluation for model, prompt, parser, embedding, and index changes.

### Security and privacy

- Threat-model each new data flow.
- Automate dependency, container, secret, and static analysis.
- Minimize personal and portfolio data and test deletion.

### Compliance

- Review claims, UX language, decision-support labels, disclaimers, and marketing.
- Reassess applicable SEBI obligations when features or operating models change.

### Developer experience

- Keep setup reproducible.
- Maintain API schemas, ADRs, runbooks, and contribution guidance.
- Keep feedback loops fast without weakening release gates.

## 10. Immediate Phase 1 Decision Backlog

| Decision | Why it matters |
| --- | --- |
| Filing and exchange source strategy | Authority, reliability, access, and licensing |
| Live market-data provider and latency definition | Product claims, cost, and freshness |
| Transcript provider | Coverage, rights, and citation granularity |
| Identity provider | Authentication and tenant isolation |
| Document storage | Durability, cost, and local/production parity |
| Parser/OCR approach | Table quality and citation location |
| Async job mechanism | Reliability and service coupling |
| Production deployment target | Networking, secret management, scaling, operations |
| Compliance operating model | SEBI obligations and permissible product language |

## 11. Definition of Done

A roadmap item is complete only when:

- acceptance criteria are met;
- tests and security controls are implemented;
- observability and failure behavior are defined;
- documentation and API contracts are current;
- no secrets or sensitive fixtures are committed;
- AI changes pass relevant evaluation gates;
- user-facing financial output includes source, freshness, assumptions, risk, and required disclaimer.

## 12. Related Documents

- [Product Requirements](PRD.md)
- [High-Level Design](HLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [Testing Strategy](TESTING_STRATEGY.md)
- [Security](SECURITY.md)
