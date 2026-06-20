# MarketMind AI

MarketMind AI is an AI-powered investment research and portfolio assistant. It is designed to collect company filings, analyze annual reports, quarterly results, and earnings-call transcripts, track market data, monitor portfolios, and provide evidence-based **buy/watch/hold/rotate decision support**.

MarketMind AI is a research tool—not an automatic trading system. It does not place orders, guarantee returns, or replace a qualified financial professional.

## Product Vision

Investment research is fragmented across regulatory filings, financial statements, transcripts, market feeds, and personal portfolio records. MarketMind AI aims to bring these sources into one traceable research workspace.

The planned experience includes:

- source-grounded company and industry research;
- retrieval-augmented generation with document citations;
- financial statement, valuation, and risk analysis;
- portfolio exposure, concentration, and performance insights;
- specialist Filing, Financial, Valuation, Risk, and Portfolio agents;
- CIO Agent synthesis that preserves uncertainty and contrary evidence;
- transparent data timestamps, assumptions, calculations, and limitations.

All recommendation-like output remains non-binding decision support.

## Current Status

The repository currently includes:

- Phase 1 product, architecture, AI/RAG, security, API, and testing documentation;
- a Java 21 Spring Boot backend foundation;
- a Clean Architecture Company Master CRUD module;
- PostgreSQL Flyway migrations;
- local PostgreSQL, Redis, Qdrant, and pgAdmin infrastructure;
- Kubernetes and Helm deployment foundations.

The frontend and AI service directories are reserved for later implementation.

## Tech Stack

| Area | Technology |
| --- | --- |
| Frontend | React, TypeScript, Vite |
| Backend | Java 21, Spring Boot 3, Maven |
| Backend APIs | Spring Web, Validation, OpenAPI |
| Persistence | Spring Data JPA, PostgreSQL 16, Flyway |
| AI service | Python, FastAPI |
| Vector database | Qdrant |
| Cache/coordination | Redis 7 |
| Local models | Ollama, `llama3.1`, `nomic-embed-text` |
| Local infrastructure | Docker Compose |
| Kubernetes packaging | Helm |

## Repository Structure

```text
MarketMindAI/
├── architecture/     # Deployment and architecture notes
├── backend/          # Spring Boot API and domain modules
├── frontend/         # React + TypeScript + Vite application
├── ai-service/       # FastAPI RAG and multi-agent service
├── database/         # Shared database tooling and artifacts
├── datasets/         # Development and AI evaluation datasets
├── docker/           # Local environment configuration and guidance
├── docs/             # Product and engineering specifications
├── helm/             # MarketMind AI Helm chart
├── infrastructure/   # Cloud infrastructure definitions
├── kubernetes/       # Kubernetes deployment documentation
├── monitoring/       # Observability configuration
├── prompts/          # Versioned AI prompt templates
└── scripts/          # Development and operational automation
```

## Local Setup

### Prerequisites

- Docker Engine or Docker Desktop with Docker Compose
- Java 21
- Maven 3.9+

Verify the tools:

```bash
docker compose version
java -version
mvn -version
```

### 1. Configure the local environment

```bash
cp docker/.env.example docker/.env
```

The committed values are development-only defaults. `docker/.env` is ignored by Git.

### 2. Start local infrastructure

From the repository root:

```bash
docker compose --env-file docker/.env up -d
docker compose --env-file docker/.env ps
```

This starts:

- PostgreSQL at `127.0.0.1:5432`
- Redis at `127.0.0.1:6379`
- Qdrant HTTP/gRPC at `127.0.0.1:6333/6334`
- pgAdmin at `http://localhost:5050`

If an existing PostgreSQL volume was initialized with different credentials and its local data can be discarded:

```bash
docker compose --env-file docker/.env down -v
docker compose --env-file docker/.env up -d
```

The first command permanently deletes local Compose volumes.

See [docker/README.md](docker/README.md) for health checks, persistence, troubleshooting, and authenticated Qdrant access.

### 3. Run the backend

From `backend/`:

```bash
mvn spring-boot:run
```

Flyway applies database migrations automatically, and Hibernate validates the resulting schema.

Verify the service:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/companies
```

OpenAPI endpoints are available during local development:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### 4. Build and test

```bash
cd backend
mvn clean verify
```

## Kubernetes and Helm

The Helm chart is located at `helm/marketmind-ai`.

```bash
helm lint ./helm/marketmind-ai
helm template marketmind ./helm/marketmind-ai
```

See [kubernetes/README.md](kubernetes/README.md) for local-image loading, Secret handling, persistence, Redis, Ingress, and installation guidance.

## Documentation

- [Product requirements](docs/PRD.md)
- [High-level design](docs/HLD.md)
- [Low-level design](docs/LLD.md)
- [AI and RAG design](docs/AI_RAG_DESIGN.md)
- [Security baseline](docs/SECURITY.md)
- [API guidelines](docs/API_GUIDELINES.md)
- [Coding standards](docs/CODING_STANDARDS.md)
- [Testing strategy](docs/TESTING_STRATEGY.md)
- [Development roadmap](docs/ROADMAP.md)

## Security

**Never commit API keys, passwords, access tokens, private keys, certificates, or populated `.env` files.**

- Commit only sanitized templates such as `.env.example`.
- Use environment variables for local overrides.
- Use an approved secret manager for shared and production environments.
- Keep databases and model infrastructure off the public network.
- Rotate any credential exposed in source control, logs, shell history, or tickets.
- Treat portfolio data, identity data, and financial documents as sensitive.

Local example credentials are predictable development defaults and must never be reused outside an isolated workstation.

## Compliance Disclaimer

MarketMind AI is intended for informational, educational, and research purposes only. It is not registered with the Securities and Exchange Board of India (SEBI) as an investment adviser or research analyst unless explicitly stated otherwise. Nothing in the product constitutes investment advice, an offer, or a solicitation to buy or sell securities. Investments in securities markets are subject to market risks; users should perform independent due diligence and consult a qualified SEBI-registered professional where appropriate.
