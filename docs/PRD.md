# MarketMind AI — Product Requirements Document

**Status:** Draft for Phase 1

**Version:** 0.1

**Audience:** Product, engineering, AI, security, compliance, and QA teams

## 1. Product Summary

MarketMind AI is an AI-powered investment research and portfolio assistant. It collects public company filings and disclosures, reads annual reports, quarterly results, and earnings-call transcripts, tracks market data, monitors user portfolios, and produces evidence-based decision support.

The product helps users investigate whether an investment merits a **buy, watch, hold, or rotate review**. These labels are research-oriented outputs, not orders, personalized investment advice, or promises of return. MarketMind AI will not place trades or automatically rebalance portfolios.

## 2. Product Vision

Create a trusted research workspace in which investors can move from raw disclosures and market data to a traceable investment thesis. Every material factual claim should be attributable to a source, every calculation should expose its assumptions, and every recommendation-like output should communicate uncertainty, risks, and data freshness.

## 3. Problem Statement

Investment research is fragmented across exchange filings, annual reports, financial statements, transcripts, market feeds, spreadsheets, and news. Users spend substantial time gathering information before they can compare companies or assess portfolio impact. General-purpose AI can accelerate reading, but unsupported answers, stale data, missing context, and hidden assumptions make it unsafe for serious financial research.

MarketMind AI addresses this by combining structured financial data, source-grounded document retrieval, portfolio analytics, and specialist AI agents in one auditable workflow.

## 4. Target Users

### 4.1 Primary users

- Self-directed investors who research listed Indian companies
- Research-oriented users maintaining watchlists or model portfolios
- Analysts who need faster document review and cross-period comparisons

### 4.2 Secondary users

- Portfolio reviewers who need concentration and risk summaries
- Internal product, AI, and compliance teams evaluating answer quality

### 4.3 User assumptions

Users understand that investing involves risk and that they remain responsible for all decisions. The initial product is not designed for discretionary portfolio management, order execution, high-frequency use, or regulated advisory workflows.

## 5. Goals and Success Measures

| Goal | Initial measure |
| --- | --- |
| Reduce research time | Users can answer common filing questions without manually searching an entire document |
| Improve traceability | Material document-derived claims include usable citations |
| Improve analytical consistency | Key ratios and period comparisons use deterministic calculations where possible |
| Make uncertainty visible | Answers disclose missing data, stale inputs, assumptions, and conflicting evidence |
| Support portfolio context | Users can see company insights alongside exposure, concentration, and risk |
| Prevent unsafe automation | No feature can submit an order or trigger an automatic rebalance |

Phase-specific targets and service-level objectives will be added after baseline usability and retrieval evaluations.

## 6. Scope

### 6.1 In scope

- Company discovery and security master data
- Ingestion of annual reports, quarterly results, exchange filings, and earnings-call transcripts
- Document parsing, metadata extraction, indexing, retrieval, and citation
- Market-price and fundamental-data ingestion from approved providers
- Company research pages and conversational research
- Period-over-period financial analysis and ratio calculations
- Watchlists and user-managed portfolios
- Holdings, transactions, allocation, performance, and concentration views
- Evidence-backed buy/watch/hold/rotate decision-support reports
- Multi-agent analysis coordinated by a CIO Agent
- Auditability, source provenance, data timestamps, and feedback capture

### 6.2 Out of scope for the initial product

- Brokerage connectivity, order placement, or trade execution
- Automatic rebalancing or autonomous portfolio management
- Guaranteed returns, price targets presented as facts, or definitive investment instructions
- Tax, legal, accounting, or personalized regulated financial advice
- Unverified social-media sentiment as a primary evidence source
- Intraday or high-frequency trading systems
- Derivatives strategy execution

## 7. Core User Journeys

### 7.1 Research a company

1. User searches for a company or security.
2. Product shows current data freshness and available source documents.
3. User asks a question or selects a research template.
4. Relevant structured data and document passages are retrieved.
5. Specialist agents analyze filings, financials, valuation, and risks.
6. The CIO Agent synthesizes the evidence.
7. The user receives a cited answer with assumptions, counter-evidence, uncertainty, and a disclaimer.

### 7.2 Compare reporting periods

1. User selects a company and two or more periods.
2. Product extracts comparable metrics from authoritative disclosures.
3. Deterministic services calculate changes and ratios.
4. The answer explains material movements with citations to the source periods.

### 7.3 Review a portfolio

1. User creates a portfolio and enters holdings or transactions.
2. Product calculates exposure, allocation, performance, and concentration.
3. Portfolio and Risk Agents identify notable changes and risk factors.
4. The CIO Agent presents prioritized review items without initiating any transaction.

### 7.4 Evaluate a decision-support label

1. User requests a buy/watch/hold/rotate review.
2. Product checks data completeness and freshness.
3. Agents produce independent evidence packets.
4. The CIO Agent reconciles conflicts and returns:
   - a non-binding label;
   - rationale and counter-thesis;
   - confidence and uncertainty;
   - valuation assumptions;
   - portfolio impact, when available;
   - citations and timestamps;
   - a clear decision-support disclaimer.

## 8. Functional Requirements

### FR-1: Identity and access

- Users shall be able to authenticate securely.
- Users shall access only their own portfolios, saved research, and preferences.
- Privileged operational functions shall require role-based authorization.

### FR-2: Company and instrument data

- The system shall maintain canonical identifiers for companies and listed instruments.
- Data from multiple providers shall be normalized while preserving provider provenance.
- UI and API responses shall expose an `as_of` timestamp where freshness matters.

### FR-3: Document ingestion

- The system shall ingest approved public disclosures and transcripts.
- Each document shall retain source URL, publisher, publication date, reporting period, document type, checksum, and ingestion status.
- Duplicate or superseded documents shall be identifiable.
- Parsing failures shall be visible and retryable.

### FR-4: Retrieval and citations

- Research answers shall retrieve from authorized, indexed sources.
- Material document-derived claims shall include citations with document and location metadata.
- The system shall abstain or qualify the answer when evidence is insufficient.
- Retrieved content shall be treated as untrusted data, not instructions.

### FR-5: Financial analysis

- Deterministic code shall calculate financial ratios and portfolio metrics where practical.
- Calculations shall record input values, period, unit, currency, formula version, and timestamp.
- The AI may explain calculations but shall not silently invent or alter inputs.

### FR-6: Multi-agent research

The AI service shall support these bounded roles:

- **Filing Analyst:** extracts material disclosures, management commentary, commitments, and changes.
- **Financial Analyst:** assesses statements, growth, margins, cash flow, leverage, and quality of earnings.
- **Valuation Analyst:** evaluates valuation using explicit assumptions and scenario ranges.
- **Risk Analyst:** identifies business, financial, governance, market, liquidity, and portfolio risks.
- **Portfolio Agent:** evaluates position size, exposure, diversification, and portfolio fit.
- **CIO Agent:** synthesizes agent evidence, resolves conflicts, and produces the final decision-support report.

Agents shall exchange structured evidence, not unrestricted hidden conclusions. The CIO Agent shall not override missing evidence with speculation.

### FR-7: Portfolio monitoring

- Users shall be able to create portfolios, holdings, and transactions.
- The system shall calculate allocation, unrealized/realized performance, concentration, and selected risk metrics.
- Market-driven alerts shall state their trigger, source data, and timestamp.
- No alert shall automatically cause a trade.

### FR-8: Explainability and feedback

- Answers shall distinguish facts, calculations, assumptions, and AI interpretation.
- Users shall be able to inspect citations and report incorrect or unsupported output.
- The system shall log model, prompt, retrieval, and policy versions needed for internal audit, subject to privacy controls.

## 9. Non-Functional Requirements

| Category | Requirement |
| --- | --- |
| Security | Follow least privilege, secure defaults, secret management, input validation, and encryption in transit |
| Privacy | Minimize personal and portfolio data; define retention and deletion controls |
| Reliability | Ingestion and analysis jobs must be idempotent and retryable |
| Performance | Interactive APIs should use pagination and bounded workloads; long AI jobs should run asynchronously |
| Availability | Degraded dependencies should produce transparent partial results rather than fabricated completeness |
| Auditability | Preserve source, model, prompt, calculation, and decision-support provenance |
| Accessibility | Target WCAG 2.2 AA for the web experience |
| Observability | Emit structured logs, metrics, traces, and correlation IDs without leaking sensitive content |
| Portability | Run the initial local platform through Docker Compose |

## 10. Data and Source Policy

Source priority, subject to licensing:

1. Regulatory and exchange disclosures
2. Company-published reports and presentations
3. Licensed market and fundamental-data providers
4. Reputable transcripts and news providers
5. User-provided documents, clearly labeled as such

The product shall not imply that all sources are complete or error-free. Conflicting facts must be surfaced, and stale sources must not be presented as current.

## 11. Compliance and Responsible-Use Requirements

- MarketMind AI is a research and decision-support product, not an automatic trading system.
- Outputs must not be framed as guaranteed, risk-free, or certain.
- The product must avoid manipulative urgency and must present material risks and counter-evidence.
- Decision-support labels must include the basis, horizon, assumptions, data timestamp, and uncertainty.
- Marketing and in-product language shall be reviewed before implying any regulated advisory service.

### SEBI disclaimer

MarketMind AI is intended for informational, educational, and research purposes only. It is not registered with the Securities and Exchange Board of India (SEBI) as an investment adviser or research analyst unless explicitly stated otherwise. Nothing in the product constitutes investment advice, a research recommendation under applicable law, an offer, or a solicitation to buy or sell securities. Users must conduct their own due diligence and consult a SEBI-registered investment adviser or other qualified professional where appropriate. Investments in securities markets are subject to market risks; past performance does not guarantee future results.

This wording is a product requirement, not legal advice. Qualified counsel must review the final product, operating model, data licenses, disclosures, and applicable SEBI obligations before launch.

## 12. Key Risks and Mitigations

| Risk | Product mitigation |
| --- | --- |
| Hallucinated or unsupported claims | Ground answers in retrieved evidence, require citations, and permit abstention |
| Stale or inconsistent market data | Display source and `as_of`; validate provider reconciliation |
| Prompt injection in documents | Treat documents as untrusted, isolate instructions, and constrain tools |
| Misleading recommendation language | Use non-binding decision-support labels with risks and disclaimers |
| Calculation errors | Use deterministic, versioned calculation services and reconciliation tests |
| Overconfidence from multi-agent consensus | Preserve dissent, evidence quality, and uncertainty; do not equate agreement with truth |
| Sensitive portfolio exposure | Enforce tenant isolation, minimization, encryption, and access auditing |
| Data licensing breach | Maintain approved-source registry and retention/use restrictions |

## 13. Dependencies and Open Decisions

- Approved filing, transcript, fundamental, and live market-data providers
- Authentication and identity strategy
- Portfolio import formats and supported asset classes
- Market-data latency definition for “live”
- Legal classification and launch jurisdictions
- Data retention, deletion, and backup policies
- Evaluation thresholds for citation correctness, faithfulness, and abstention
- Production deployment platform beyond local Docker Compose

## 14. Acceptance Criteria for Phase 1

Phase 1 is complete when:

- Product scope, personas, journeys, exclusions, and compliance posture are documented.
- Service boundaries and data ownership are defined.
- RAG ingestion, retrieval, citation, and evaluation designs are documented.
- Multi-agent responsibilities and CIO synthesis rules are documented.
- API, coding, security, and testing standards are agreed.
- The initial architecture decision is recorded.
- No application code or secrets have been introduced.

## 15. Related Documents

- [High-Level Design](HLD.md)
- [Low-Level Design](LLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [Roadmap](ROADMAP.md)
- [Security](SECURITY.md)
- [Testing Strategy](TESTING_STRATEGY.md)
