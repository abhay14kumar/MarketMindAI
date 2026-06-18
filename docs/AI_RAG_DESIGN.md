# MarketMind AI — AI, RAG, and Multi-Agent Design

**Status:** Draft for Phase 1

**Initial runtime:** Ollama

**Generation model:** `llama3.1`

**Embedding model:** `nomic-embed-text`

**Vector database:** Qdrant

## 1. Objectives

The AI subsystem must:

- answer investment-research questions from approved evidence;
- cite the sources behind material factual claims;
- combine document evidence with deterministic financial and portfolio calculations;
- identify uncertainty, contradictions, stale data, and missing evidence;
- support bounded specialist agents and a final CIO synthesis;
- abstain instead of inventing facts;
- remain a decision-support system and never execute trades.

The goal is not to make an LLM sound confident. The goal is to make research traceable, testable, and appropriately cautious.

## 2. Non-Goals

- Autonomous order placement or portfolio rebalancing
- Hidden personalized advice presented as certainty
- Using model memory as the source of current prices or filing facts
- Allowing retrieved documents to define system behavior or invoke tools
- Replacing deterministic financial calculations with free-form generation
- Treating agent agreement as proof
- Persisting unrestricted chain-of-thought

## 3. AI System Boundaries

The FastAPI service owns AI orchestration, retrieval, ingestion transformations, and AI evaluation. It does not own user authentication, canonical portfolio transactions, or authoritative market data.

The Spring Boot backend sends a bounded research request containing authorization-approved scope and receives structured results. The AI service may request only the minimum structured context required for the job.

## 4. Source and Evidence Hierarchy

Subject to licensing and availability:

1. Regulatory/exchange disclosures
2. Audited annual reports and company-filed financial results
3. Official company presentations and earnings-call materials
4. Licensed transcripts, market data, and fundamentals
5. Reputable secondary sources
6. User-provided documents, isolated and clearly labeled

Sources receive metadata for authority, recency, document type, reporting period, and visibility. Retrieval scores do not replace source-quality policy.

## 5. Ingestion Pipeline

```text
Discover -> approve source -> acquire -> validate -> checksum
         -> parse/OCR -> normalize -> segment -> chunk
         -> embed -> index -> verify -> publish
```

### 5.1 Discovery and acquisition

- Accept only allowlisted providers or explicitly authorized user uploads.
- Respect licensing, robots/access constraints, and rate limits.
- Record canonical URL, publisher, publication time, reporting period, content type, and acquisition time.
- Use checksums and provider identifiers to detect duplicates and revisions.

### 5.2 Validation

- Enforce file type and size limits.
- Validate MIME type independently of file extension.
- Scan uploads using the approved security process.
- Reject encrypted, malformed, or unsupported documents unless a controlled workflow exists.
- Mark provisional or corrected filings distinctly.

### 5.3 Parsing and normalization

- Preserve page numbers, headings, tables, footnotes, and section hierarchy.
- Use layout-aware parsing for PDF documents.
- OCR only when text extraction is insufficient; record OCR confidence and engine version.
- Normalize whitespace without changing financial values.
- Preserve the original artifact and extraction provenance.
- Detect tables and store a text representation linked to the source location.

### 5.4 Chunking

Chunking is section-aware rather than fixed-size only.

Recommended initial policy:

- target 500–900 tokens per narrative chunk;
- overlap 10–15% where section continuity requires it;
- keep table title, headers, units, and footnotes together;
- do not combine unrelated reporting periods;
- prepend compact metadata such as company, document type, fiscal period, and section;
- assign stable, versioned chunk IDs.

Exact values are configuration and must be tuned through retrieval evaluation.

### 5.5 Embedding and indexing

- Generate embeddings with `nomic-embed-text` through Ollama.
- Batch within memory and latency limits.
- Record model name, model digest/version when available, preprocessing version, vector dimension, and timestamp.
- Store vectors in Qdrant with filterable provenance and visibility payload.
- Validate vector count and sampled retrieval before marking ingestion complete.
- Use collection aliases to support re-embedding and rollback.

## 6. Retrieval Pipeline

```text
Question
  -> classify intent and required freshness
  -> resolve entities and reporting periods
  -> apply authorization/source filters
  -> retrieve semantic candidates
  -> optional lexical/hybrid candidates
  -> deduplicate and rerank
  -> diversify by source/period
  -> assemble bounded evidence context
  -> answer or abstain
```

### 6.1 Query planning

The planner identifies:

- companies and instruments;
- requested time horizon and fiscal periods;
- document types;
- metrics or topics;
- whether live/near-live market data is required;
- whether portfolio context is authorized;
- which specialist agents are needed.

Entity resolution must not silently choose between ambiguous securities.

### 6.2 Retrieval

- Apply tenant/visibility, company, document type, date, and reporting-period filters.
- Retrieve a bounded candidate set from Qdrant.
- Consider hybrid lexical retrieval for exact financial terms, names, and figures.
- Deduplicate near-identical chunks.
- Prefer authoritative and current sources when relevance is comparable.
- Preserve contrary evidence and multiple periods when the question requires comparison.

### 6.3 Reranking and context assembly

The initial system may use deterministic scoring plus semantic similarity. A dedicated reranker is deferred.

Context assembly:

- groups related chunks by document/section;
- avoids spending the full context budget on duplicate text;
- includes source identifiers and exact locations;
- includes structured calculations separately from document passages;
- never labels retrieved text as trusted instructions.

### 6.4 Retrieval confidence

Signals may include:

- semantic relevance;
- source authority;
- recency and period match;
- cross-source agreement;
- citation location quality;
- coverage of the requested subquestions.

Low confidence triggers a narrowed answer, clarifying question, or abstention.

## 7. Grounded Generation and Citations

### 7.1 Answer rules

The generation prompt requires the model to:

- use only supplied evidence for document-specific factual claims;
- distinguish facts, calculations, assumptions, and interpretation;
- cite each material factual claim;
- state when sources conflict;
- avoid making current-data claims without a current timestamped source;
- avoid precise forecasts unless they are clearly scenario assumptions;
- include risks, counter-evidence, and missing information;
- omit a decision-support label when evidence policy is not met.

### 7.2 Citation model

Each evidence item has a stable ID. The model references evidence IDs in structured output; the application resolves them to user-visible citations.

A citation contains:

- document title and type;
- publisher/source;
- reporting/publication date;
- page and section, or transcript timestamp/segment;
- canonical URL or internal document-view route;
- data `as_of` where applicable.

### 7.3 Citation validation

Before publication:

1. Parse claims and cited evidence references.
2. Confirm every reference was supplied to the model and is authorized.
3. Verify the cited passage supports the claim using deterministic checks and/or a separately evaluated verifier.
4. Check citation coverage for material claims.
5. Reject, revise, or abstain when validation fails.

Citation presence alone is not grounding; entailment and source quality matter.

## 8. Multi-Agent Design

Agents are bounded analysis roles sharing a structured evidence workspace. They are not independent autonomous actors with unrestricted tools.

### 8.1 Filing Analyst

Focus:

- material disclosures and changes across periods;
- management commentary and guidance;
- auditor qualifications, contingent liabilities, related-party disclosures;
- capital allocation, governance, and stated commitments.

Output includes quoted/extracted evidence references and unresolved disclosure questions.

### 8.2 Financial Analyst

Focus:

- revenue, margin, earnings, cash flow, working capital, leverage, and return metrics;
- period comparisons and quality of earnings;
- reconciliation between narrative claims and reported statements.

All calculations use approved deterministic inputs and formulas.

### 8.3 Valuation Analyst

Focus:

- valuation multiples and scenario-based methods;
- peer or historical comparisons where data permits;
- explicit growth, margin, discount-rate, and terminal assumptions;
- sensitivity ranges rather than false precision.

Valuation outputs are scenarios, not guaranteed price targets.

### 8.4 Risk Analyst

Focus:

- business, industry, financial, governance, regulatory, liquidity, and market risks;
- source conflicts, data gaps, and thesis failure conditions;
- concentration and downside considerations.

The Risk Analyst is expected to challenge the emerging thesis.

### 8.5 Portfolio Agent

Focus:

- current position size and exposure;
- sector, issuer, factor, and liquidity concentration;
- portfolio fit, overlap, and effect of hypothetical changes;
- constraints supplied by the user.

It may simulate a hypothetical allocation but cannot perform or authorize a trade.

### 8.6 CIO Agent

Responsibilities:

- validate that required specialist packets are present;
- compare evidence quality and freshness;
- reconcile disagreements without suppressing dissent;
- synthesize thesis, counter-thesis, risks, valuation assumptions, and portfolio context;
- select or omit a buy/watch/hold/rotate review label according to policy;
- produce the final structured, cited report.

The CIO Agent cannot introduce unsupported facts. It must expose major disagreement and downgrade confidence when evidence conflicts.

## 9. Orchestration

### 9.1 Routing

Not every question invokes every agent.

| Question type | Typical agents |
| --- | --- |
| Locate a filing fact | Filing Analyst |
| Explain financial trend | Filing + Financial |
| Assess valuation | Financial + Valuation + Risk |
| Review portfolio position | Portfolio + Risk |
| Full investment review | Filing + Financial + Valuation + Risk + Portfolio, then CIO |

### 9.2 Workflow

```text
Policy and scope check
        |
Query planning and retrieval
        |
Parallel specialist analysis where independent
        |
Evidence and calculation validation
        |
CIO synthesis
        |
Citation and safety validation
        |
Publish, qualify, or abstain
```

Agent prompts, tools, schemas, and time budgets are versioned. Retries are bounded. A partial report names unavailable analyses rather than fabricating them.

### 9.3 Structured output

Agents produce schema-validated JSON with:

- findings;
- evidence IDs;
- calculation IDs;
- assumptions;
- counter-evidence;
- unknowns;
- confidence band;
- `as_of`.

Only the final rendering layer turns this into user-facing prose.

## 10. Decision-Support Policy

Allowed labels:

- **Buy review:** evidence suggests the opportunity may merit consideration under stated assumptions.
- **Watch:** potentially interesting, but a catalyst, valuation, evidence, or risk threshold is unresolved.
- **Hold review:** existing exposure may remain consistent with the stated thesis; not an instruction to retain.
- **Rotate review:** the position or capital allocation may merit reassessment relative to alternatives or portfolio constraints.

Every label:

- is non-binding and optional;
- states time horizon and assumptions;
- includes strongest supporting and opposing evidence;
- includes confidence and missing data;
- includes market-data and source timestamps;
- includes the approved disclaimer;
- never triggers an order.

## 11. Prompt-Injection and Tool Safety

- System/developer policies are separated from retrieved content.
- Documents and user text are explicitly marked as untrusted data.
- Instructions found in documents are ignored.
- Agents receive allowlisted tools with typed inputs and least privilege.
- Retrieval cannot expand beyond authorized tenant/source filters.
- URLs and downloads are validated against source policy and network protections.
- Model output is schema-validated and treated as untrusted until checked.
- Sensitive values are never inserted into prompts unless strictly necessary and approved.

## 12. Evaluation Strategy

### 12.1 Offline datasets

Curated cases should include:

- direct filing questions with known page-level evidence;
- multi-period financial comparisons;
- table extraction and unit/currency traps;
- conflicting or amended disclosures;
- stale-data questions;
- insufficient-evidence cases;
- prompt-injection content;
- valuation scenarios;
- portfolio concentration cases;
- questions where abstention is the correct behavior.

### 12.2 Metrics

| Layer | Example metrics |
| --- | --- |
| Parsing | text/table extraction accuracy, metadata accuracy |
| Retrieval | recall@k, precision@k, MRR/nDCG, period/source filter accuracy |
| Grounding | claim faithfulness, citation precision, citation coverage |
| Answer quality | correctness, completeness, relevance, uncertainty calibration |
| Safety | prompt-injection resistance, data leakage, disallowed advice rate |
| Agents | schema validity, evidence use, disagreement preservation |
| Operations | latency, failure rate, context size, throughput |

Human financial-domain review is required for high-risk golden cases.

### 12.3 Release gates

A model, prompt, chunking, or retrieval change is not promoted when it:

- materially reduces citation correctness or retrieval recall;
- increases unsupported claims or unsafe recommendation language;
- fails injection or tenant-isolation tests;
- changes calculation interpretation without review;
- lacks reproducible evaluation metadata.

Thresholds will be established after a baseline dataset is built.

## 13. Monitoring and Feedback

Production-oriented monitoring should track:

- retrieval with zero/low-quality evidence;
- citation validation failures;
- abstention and regeneration rates;
- stale-data usage;
- agent failures and disagreement;
- user-reported unsupported claims;
- latency and resource saturation;
- drift by document type and reporting period.

User feedback enters an adjudicated evaluation backlog; it does not automatically train or alter prompts.

## 14. Model and Prompt Lifecycle

- Pin and record model names/digests where possible.
- Version prompts, response schemas, retrieval configuration, and policy.
- Evaluate all changes against the same baseline before comparison.
- Keep rollback paths for Qdrant collection aliases and prompt/model configuration.
- Do not use private user content for training without explicit policy and consent.

## 15. Compliance Disclaimer

All research outputs must include or link prominently to the approved disclaimer:

> MarketMind AI is for informational, educational, and research purposes only. It is not a SEBI-registered investment adviser or research analyst unless explicitly stated otherwise, and its outputs do not constitute investment advice, an offer, or a solicitation. Investments in securities markets are subject to market risks. Users should perform independent due diligence and consult a qualified SEBI-registered professional where appropriate.

Final wording and placement require legal review.

## 16. Open Decisions

- Hybrid retrieval and reranking implementation
- Parser/OCR and document-storage choices
- Exact context, chunk, and agent work budgets
- Confidence calibration method
- Human-review workflow for low-confidence high-impact reports
- Licensed market, filing, and transcript providers
- Model upgrade policy beyond the local baseline

## 17. Related Documents

- [Product Requirements](PRD.md)
- [High-Level Design](HLD.md)
- [Low-Level Design](LLD.md)
- [Security](SECURITY.md)
- [Testing Strategy](TESTING_STRATEGY.md)
