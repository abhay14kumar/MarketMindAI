# MarketMind AI

MarketMind AI is an AI-powered investment research and portfolio assistant designed to help investors turn fragmented market information into clear, evidence-backed insights.

The platform is intended to combine market data, company fundamentals, financial documents, portfolio analytics, and retrieval-augmented generation (RAG) in one research workspace. MarketMind AI should help users investigate opportunities, understand portfolio risk, and make more informed decisions without presenting AI-generated output as financial advice.

> **Project status:** Early planning and architecture. The repository currently contains the proposed monorepo structure and documentation placeholders; application services have not yet been implemented.

## Product Vision

MarketMind AI aims to make high-quality investment research more accessible, explainable, and efficient.

The planned product experience includes:

- A conversational research assistant grounded in trusted financial sources
- Company, industry, and market research with source citations
- Portfolio tracking, allocation analysis, and risk insights
- Financial document ingestion and semantic search
- Watchlists, alerts, and personalized research workflows
- Transparent AI responses that separate sourced facts, calculations, and generated analysis

MarketMind AI is a decision-support tool. It should augment investor research—not execute trades, guarantee outcomes, or replace a licensed financial professional.

## Proposed Tech Stack

The final technology choices will be validated during the architecture phase. The current target stack is:

| Area | Proposed technologies | Purpose |
| --- | --- | --- |
| Web application | Next.js, React, TypeScript | Research dashboard and portfolio experience |
| Backend API | Python, FastAPI | Authentication, portfolio, research, and data APIs |
| AI service | Python, LLM APIs, RAG framework | Retrieval, orchestration, prompting, and response generation |
| Data layer | PostgreSQL, pgvector, Redis | Relational data, vector search, caching, and background jobs |
| Market data | Pluggable third-party providers | Prices, fundamentals, filings, and market news |
| Infrastructure | Docker, Kubernetes, Infrastructure as Code | Repeatable local, staging, and production environments |
| Observability | OpenTelemetry, Prometheus, Grafana | Logs, metrics, traces, and service health |
| Quality | Automated tests, linting, type checking, CI/CD | Safe and consistent delivery |

Provider-specific integrations and framework choices should remain replaceable where practical to reduce vendor lock-in.

## Repository Structure

```text
MarketMindAI/
├── frontend/         # Web application and user interface
├── backend/          # Core API, business logic, and integrations
├── ai-service/       # RAG pipeline, model orchestration, and AI evaluation
├── database/         # Schemas, migrations, seeds, and database tooling
├── datasets/         # Development and evaluation dataset definitions
├── prompts/          # Versioned prompt templates and prompt documentation
├── docs/             # Product and system design documents
│   ├── PRD.md
│   ├── HLD.md
│   ├── LLD.md
│   ├── AI_RAG_DESIGN.md
│   └── ROADMAP.md
├── docker/           # Container definitions and local orchestration
├── infrastructure/   # Cloud infrastructure as code
├── kubernetes/       # Kubernetes manifests and deployment configuration
├── monitoring/       # Dashboards, alerts, and observability configuration
├── scripts/          # Development, maintenance, and automation scripts
└── .vscode/          # Shared editor configuration
```

## Local Setup Plan

Local development is not available yet because application code and dependency manifests have not been added. The intended setup flow is:

1. Install the required runtimes and tools:
   - Node.js and a selected package manager
   - Python and a selected environment/package manager
   - Docker with Docker Compose
2. Clone the repository and create local environment files from committed example files such as `.env.example`.
3. Start PostgreSQL, Redis, and any local observability dependencies with Docker Compose.
4. Apply database migrations and load development seed data.
5. Install dependencies for `frontend/`, `backend/`, and `ai-service/`.
6. Configure development-only market-data and model-provider credentials.
7. Start each service through a root-level development command.
8. Run linting, type checks, unit tests, integration tests, and AI evaluation suites before submitting changes.

As implementation begins, this section should be replaced with exact, copy-and-paste commands and documented version requirements.

## Development Roadmap

### Phase 1 — Product and Architecture

- Define the product requirements and initial user journeys
- Complete high-level, low-level, data, and RAG architecture designs
- Select data providers, model providers, and deployment targets
- Establish engineering standards, CI checks, and threat modeling

### Phase 2 — Platform Foundation

- Scaffold the frontend, backend, and AI services
- Add authentication, authorization, and user settings
- Implement database schemas, migrations, and local containers
- Establish logging, metrics, tracing, tests, and deployment pipelines

### Phase 3 — Research MVP

- Add company search, market data, fundamentals, and filings
- Build document ingestion, chunking, embedding, and retrieval
- Deliver cited conversational research and saved research sessions
- Introduce automated RAG quality, grounding, and regression evaluations

### Phase 4 — Portfolio Intelligence

- Add portfolios, holdings, transactions, and watchlists
- Provide allocation, performance, concentration, and risk analysis
- Generate portfolio-aware research with clear assumptions and data freshness
- Add alerts and scheduled research summaries

### Phase 5 — Production Readiness

- Conduct security, privacy, reliability, and cost reviews
- Add rate limits, audit trails, backups, and disaster recovery
- Improve accessibility, performance, and mobile responsiveness
- Run a controlled beta and iterate from user feedback

## Security and Responsible Use

**Never store API keys, access tokens, passwords, private certificates, or other secrets in source code or commit them to version control.**

- Use environment variables or an approved secrets manager.
- Commit only sanitized templates such as `.env.example`.
- Keep local `.env` files and credential artifacts in `.gitignore`.
- Use separate credentials for development, staging, and production.
- Apply least-privilege access and rotate credentials regularly.
- Treat portfolio data, user identity data, and financial documents as sensitive.
- Validate external data, sanitize uploaded content, and defend RAG workflows against prompt injection.
- Require source attribution and display data timestamps where financial information may become stale.

If a secret is exposed, revoke and rotate it immediately, then remove it from the repository history.

## Documentation

Detailed specifications will live in [`docs/`](docs/):

- [`PRD.md`](docs/PRD.md) — product requirements
- [`HLD.md`](docs/HLD.md) — high-level architecture
- [`LLD.md`](docs/LLD.md) — low-level design
- [`AI_RAG_DESIGN.md`](docs/AI_RAG_DESIGN.md) — retrieval and AI system design
- [`ROADMAP.md`](docs/ROADMAP.md) — delivery milestones and priorities

## Disclaimer

MarketMind AI is intended for research and educational purposes only. It does not provide financial, investment, tax, or legal advice. Investment decisions involve risk, including the possible loss of principal.
