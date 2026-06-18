# MarketMind AI — Security and Responsible-AI Baseline

**Status:** Draft for Phase 1

**Scope:** Product, application, AI/RAG, data, local infrastructure, and software supply chain

## 1. Security Objectives

MarketMind AI must protect:

- user identity and account data;
- private portfolios, holdings, transactions, and preferences;
- licensed or user-provided documents;
- source-provider credentials and model/service credentials;
- system integrity, including financial calculations and research provenance;
- availability of research and monitoring workflows;
- users from unsupported, manipulated, or overconfident financial output.

Security is required at design, implementation, testing, deployment, and operation.

## 2. Mandatory Secret Policy

**Never store API keys, passwords, access tokens, private keys, certificates, database credentials, or other secrets in source code or commit them to version control.**

- Inject secrets through environment variables for local development and an approved secrets manager for shared environments.
- Commit only sanitized templates such as `.env.example`.
- Ignore local `.env` files, credential files, dumps, and private certificates.
- Use distinct credentials per environment and service.
- Apply least privilege, expiration where possible, and regular rotation.
- Scan commits and CI artifacts for secrets.
- If a secret is exposed, revoke/rotate it immediately, investigate access, and remove it from history using an approved incident procedure.

Ollama model names and non-sensitive endpoints may be configuration; credentials may not be.

## 3. Data Classification

| Class | Examples | Handling |
| --- | --- | --- |
| Public | Public filings, published company reports | Validate integrity and licensing; cite provenance |
| Internal | Architecture, prompts, evaluation summaries | Access-controlled; no public exposure by default |
| Confidential | User profile, private research, provider contracts | Encrypt, minimize, restrict, audit |
| Highly sensitive | Credentials, tokens, private portfolio transactions | Strong least privilege, redaction, strict retention |

Public source content can still be malicious and must be treated as untrusted.

## 4. Threat Model Summary

### 4.1 Primary actors

- Unauthenticated internet attacker
- Authenticated malicious or compromised user
- Compromised external data source
- Malicious document or prompt-injection author
- Compromised dependency/container
- Over-privileged or mistaken operator
- Misconfigured internal service

### 4.2 High-priority threats

| Threat | Example controls |
| --- | --- |
| Account takeover | Standards-based identity, MFA support, secure sessions, rate limits |
| Broken tenant isolation | Central authorization, ownership filters, negative tests |
| Injection | Parameterization, validation, output encoding, no dynamic evaluation |
| Prompt injection | Treat sources as data, bounded tools, instruction isolation, output validation |
| SSRF/source abuse | Allowlisted providers, URL validation, DNS/IP controls, egress restrictions |
| Malicious upload | Size/type validation, scanning, sandboxed parsing, storage isolation |
| Data poisoning | Approved source registry, checksums, provenance, anomaly/revision checks |
| Secret leakage | Secret manager, redaction, scans, no prompt/log inclusion |
| Unsupported financial claims | RAG citations, deterministic calculations, abstention, review gates |
| Supply-chain compromise | Lock files, dependency scanning, SBOM, signed/pinned images where feasible |
| Denial of service | Quotas, body limits, job budgets, concurrency controls, timeouts |
| Audit tampering | Restricted append-oriented audit storage and integrity monitoring |

A detailed threat model must be revisited before each major capability is released.

## 5. Identity and Authorization

- Use a standards-based identity provider; do not build password storage casually.
- Support MFA for privileged roles and, where feasible, users.
- Prefer short-lived sessions/tokens with secure renewal.
- Browser cookies, if used, are `Secure`, `HttpOnly`, and appropriately `SameSite`.
- Protect state-changing cookie-authenticated requests against CSRF.
- Enforce authorization in backend application services for every user-owned resource.
- Use deny-by-default roles and explicit administrative permissions.
- Service-to-service identity is separate from user identity.
- Never trust browser-supplied user IDs, roles, tenant IDs, or internal headers.

## 6. Network and Service Security

- Public ingress reaches only intended frontend/backend endpoints.
- PostgreSQL, Qdrant, Ollama, and the AI service stay on private networks.
- Use TLS in shared and production environments.
- Apply outbound egress restrictions to ingestion and AI workloads.
- Use timeouts, response-size limits, and safe redirects for external fetches.
- Validate internal service certificates/identity once outside local development.
- Docker Compose ports bind only where required.

## 7. Application Security

- Validate request shape, semantic rules, size, encoding, and ownership.
- Parameterize SQL and queries.
- Prevent mass assignment with explicit request schemas.
- Encode output by context; sanitize any allowed rich text.
- Configure CORS narrowly.
- Use security headers, including CSP when frontend implementation begins.
- Avoid unsafe deserialization and runtime code evaluation.
- Return safe error details.
- Apply rate limits and work budgets to expensive operations.

## 8. Document Ingestion Security

- Accept only approved sources and supported file types.
- Verify MIME/content signatures; do not trust extensions.
- Enforce file and decompressed-size limits.
- Reject archive bombs, malformed files, and unsupported encryption.
- Scan user uploads through approved malware controls.
- Parse in a constrained worker with limited filesystem, memory, CPU, network, and runtime permissions.
- Store originals outside the public web root with generated object names.
- Strip or ignore active content, macros, links, and embedded instructions.
- Preserve checksum, source, acquisition time, and parser provenance.

## 9. AI, RAG, and Agent Security

### 9.1 Prompt injection

- Mark user and retrieved content as untrusted.
- Never concatenate documents into system instructions.
- Ignore requests inside documents to reveal secrets, change rules, or invoke tools.
- Give each agent an allowlisted, typed, least-privilege tool set.
- Enforce authorization before retrieval and again before rendering citations.
- Treat model output as untrusted and schema-validate it.

### 9.2 Data leakage

- Send the minimum portfolio/document context needed.
- Do not mix tenants in retrieval collections without mandatory visibility filters and tests.
- Do not expose hidden prompts, credentials, internal URLs, or other users' evidence.
- Do not use private user data for model training without explicit policy and consent.
- Redact sensitive content from logs and telemetry.

### 9.3 Unsafe financial output

- Require citations for material factual claims.
- Use deterministic calculation services.
- Expose data freshness, assumptions, risks, counter-evidence, and uncertainty.
- Abstain when evidence is insufficient.
- Never execute a trade, generate broker instructions, or claim guaranteed returns.
- Preserve Risk Analyst dissent; CIO synthesis cannot erase material conflicts.

## 10. Data Protection

- Encrypt data in transit.
- Encrypt confidential and highly sensitive data at rest in shared/production environments.
- Minimize collection and retention.
- Define retention by data class and legal/provider requirement.
- Implement user export and deletion workflows before handling production personal data.
- Backups inherit the same access, encryption, retention, and deletion requirements.
- Test restoration and deletion behavior.
- Use synthetic or anonymized data in non-production environments.

## 11. Logging, Audit, and Privacy

Operational logs must not contain:

- passwords, secrets, tokens, cookies, authorization headers;
- full portfolio transaction details unless specifically approved;
- full documents, raw prompts, or raw user questions by default;
- unnecessary personal data.

Audit events should capture:

- authentication and authorization-sensitive actions;
- privileged configuration changes;
- portfolio mutations;
- source/document ingestion transitions;
- research job and policy versions;
- data export/deletion actions.

Audit logs are access-controlled and protected from silent alteration.

## 12. Software Supply Chain

- Pin dependency versions using ecosystem lock/build mechanisms.
- Use supported Java, Python, Node, database, Qdrant, and Ollama versions.
- Scan source, dependencies, containers, and infrastructure configuration.
- Generate an SBOM for release artifacts when implementation begins.
- Use minimal container images and non-root users.
- Do not copy local secret files into images.
- Review licenses for code, models, embeddings, datasets, filings, and transcripts.
- Patch critical vulnerabilities within the incident/vulnerability policy.

## 13. Local Docker Compose Baseline

- Use named volumes for state.
- Do not use default production-like passwords in shared environments.
- Bind databases and model/vector services to localhost only if host access is required.
- Use separate service credentials with minimal grants.
- Add health checks and startup dependency handling.
- Avoid privileged containers and Docker socket mounts.
- Limit CPU, memory, and concurrency where practical.
- Document safe reset procedures without embedding credentials.

## 14. Vulnerability and Incident Management

- Provide a private security-reporting channel before public release.
- Triage by exploitability and impact to confidentiality, integrity, availability, or financial-output safety.
- Preserve evidence and rotate affected credentials.
- Notify affected stakeholders according to legal and contractual requirements.
- Conduct root-cause analysis and add regression controls.
- Practice restore, secret exposure, provider compromise, and AI data-leak scenarios.

## 15. Compliance and SEBI Posture

MarketMind AI is designed as informational and research decision support, not as automatic trading or guaranteed advice.

Required disclaimer:

> MarketMind AI is intended for informational, educational, and research purposes only. It is not registered with SEBI as an investment adviser or research analyst unless explicitly stated otherwise. Its output is not investment advice, an offer, or a solicitation to transact in securities. Investments in securities markets are subject to market risks. Users should perform independent due diligence and consult a qualified SEBI-registered professional where appropriate.

Final legal classification, disclosures, recordkeeping, marketing language, and applicable SEBI requirements require review by qualified counsel before launch and whenever the product model changes.

## 16. Security Release Checklist

- No secrets or sensitive fixtures are committed.
- Authentication and authorization tests pass.
- Tenant isolation is tested negatively.
- External URLs and uploads are constrained.
- Dependencies and images are scanned.
- Logs and errors are reviewed for leakage.
- RAG prompt-injection and data-exfiltration tests pass.
- Citations and abstention gates pass.
- Financial calculations are reconciled.
- Backup/restore and rollback paths are documented.
- Disclaimer and compliance language are present and reviewed.

## 17. Related Documents

- [Product Requirements](PRD.md)
- [High-Level Design](HLD.md)
- [AI and RAG Design](AI_RAG_DESIGN.md)
- [Testing Strategy](TESTING_STRATEGY.md)
