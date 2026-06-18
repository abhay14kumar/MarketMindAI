# MarketMind AI — Coding Standards

**Status:** Draft for Phase 1

**Applies to:** Frontend, backend, AI service, database, scripts, tests, and infrastructure configuration

## 1. General Principles

- Optimize for correctness, clarity, security, and maintainability.
- Keep business rules explicit and testable.
- Prefer small, cohesive modules with clear ownership.
- Validate all input at trust boundaries.
- Use deterministic code for calculations and policy checks.
- Do not store secrets, credentials, tokens, or private keys in source code.
- Do not commit production data or sensitive portfolio/document fixtures.
- Keep AI output untrusted until schema, citation, and policy validation completes.
- No code path may place trades or automatically rebalance a portfolio.

## 2. Repository Rules

- Keep service-specific code in `frontend/`, `backend/`, and `ai-service/`.
- Keep shared contracts language-neutral, preferably OpenAPI/JSON Schema, rather than tightly coupling source modules.
- Keep prompts versioned under `prompts/`; prompts must not contain secrets.
- Put database migrations in `database/` or the backend migration location selected during scaffolding.
- Document significant architecture changes with ADRs.
- Generated files are either reproducibly generated and ignored or clearly labeled; do not edit them manually.

## 3. Change Quality

Every implementation change should:

- have a focused purpose;
- include relevant tests;
- update API/design documentation when contracts change;
- avoid unrelated formatting churn;
- pass formatting, linting, type checks, tests, and security scans;
- include migration and rollback considerations when data or AI indexes change.

Reviews prioritize correctness, security, financial-data integrity, grounding, and backward compatibility over style preference.

## 4. TypeScript and React

### 4.1 TypeScript

- Enable strict TypeScript settings.
- Avoid `any`; use `unknown` and narrow it safely.
- Define API contracts from generated schemas where practical.
- Prefer discriminated unions for states and results.
- Treat money as decimal strings/typed money objects, not floating-point numbers.
- Avoid non-null assertions except where an invariant is proven and documented.

### 4.2 React

- Use functional components and hooks.
- Keep feature logic in feature modules; shared components remain domain-neutral.
- Separate server state, form state, and local presentation state.
- Do not put authoritative financial calculations in components.
- Handle loading, empty, partial, stale, error, and unauthorized states.
- Use semantic HTML and meet WCAG 2.2 AA goals.
- Do not render model-produced HTML without safe sanitization.

### 4.3 Frontend tooling baseline

Final configurations will be selected during scaffolding. The intended baseline includes:

- ESLint;
- Prettier;
- strict TypeScript compiler checks;
- unit/component tests;
- accessible interaction checks.

## 5. Java and Spring Boot

### 5.1 Java

- Target Java 21.
- Use modern language features when they improve clarity.
- Use `BigDecimal` for money and precise financial calculations; define rounding explicitly.
- Prefer immutable value objects and records for data carriers where appropriate.
- Avoid returning `null` collections.
- Do not use floating point for money.
- Inject a `Clock` rather than reading system time directly in domain logic.
- Keep provider and persistence details outside domain rules.

### 5.2 Spring Boot

- Organize by feature/domain, not only technical layer.
- Controllers validate transport concerns and delegate to use cases.
- Application services define transactional boundaries.
- Repositories and external providers implement domain/application ports.
- JPA entities do not cross API boundaries.
- Use constructor injection.
- Centralize safe exception-to-problem-details mapping.
- Enforce authorization in service/application rules, not UI assumptions.
- Validate configuration at startup.

### 5.3 Maven and quality

The intended baseline includes:

- reproducible Maven builds and pinned plugin versions;
- formatting/style enforcement;
- compiler warnings and static analysis;
- unit and integration tests;
- dependency and vulnerability scanning.

Exact plugins are chosen during implementation and recorded in build documentation.

## 6. Python and FastAPI

- Use a currently supported Python 3 release selected and pinned during scaffolding.
- Require type hints for public functions and service boundaries.
- Use Pydantic models for validated request, response, and agent schemas.
- Keep I/O boundaries async only where the underlying operations support it; do not mix blocking work into the event loop.
- Separate routes, use cases, adapters, and domain/policy logic.
- Never execute model-generated code or arbitrary tool arguments.
- Use bounded retries, timeouts, and concurrency.
- Avoid mutable default arguments and hidden global state.
- Do not persist unrestricted chain-of-thought.

The intended tooling baseline includes a formatter/linter, static type checker, pytest, and security/dependency scanning.

## 7. Database and SQL

- All schema changes use versioned migrations.
- Migrations are reviewed, tested from empty and representative prior states, and safe for the target deployment.
- Use constraints to enforce invariants where possible.
- Add indexes based on query behavior, not speculation.
- Use UTC timestamps and explicit time zones.
- Use `numeric`/decimal types for financial values.
- Include source, unit, currency, period, and `as_of` for financial observations.
- Avoid destructive migrations without a staged migration and backup/rollback plan.
- Parameterize all SQL.
- Do not put secrets or sensitive production data in seed files.

## 8. API and Data Contracts

- Follow [API_GUIDELINES.md](API_GUIDELINES.md).
- Validate at ingress and egress.
- Make units and nullability explicit.
- Use stable error codes.
- Prefer additive compatible changes.
- Require idempotency for retried commands.
- Keep internal provider fields out of public contracts.

## 9. AI and Prompt Standards

- Version prompts and output schemas.
- Prompts define role, allowed evidence, tool limits, output schema, abstention behavior, and disclaimer policy.
- Retrieved text is delimited and labeled untrusted.
- Material claims require evidence IDs.
- Calculations reference deterministic calculation IDs.
- Agent outputs are schema-validated before downstream use.
- Changes to model, prompt, chunking, embedding, retrieval, or citations require evaluation.
- Never log full prompts or evidence containing sensitive data by default.
- Never claim certainty or guaranteed return.

## 10. Error Handling

- Fail explicitly with stable, actionable error categories.
- Do not catch exceptions only to ignore them.
- Retry only transient failures and use bounded exponential backoff with jitter.
- Do not retry validation, authorization, or permanent parsing failures.
- Preserve correlation and job IDs.
- User-facing errors do not expose internals, secrets, SQL, prompts, or stack traces.

## 11. Logging and Telemetry

- Use structured logs.
- Use consistent service, environment, trace, correlation, operation, outcome, and latency fields.
- Redact tokens, cookies, credentials, personal data, portfolio details, and document text.
- Do not use logging as the only audit mechanism.
- Metrics labels must be bounded; never use user IDs or raw questions as metric labels.

## 12. Security Standards

- Follow [SECURITY.md](SECURITY.md).
- Use approved libraries and pinned lock/build files.
- Scan dependencies, containers, code, and repository history for secrets.
- Avoid unsafe deserialization, dynamic evaluation, and command construction.
- Encode output according to context.
- Validate file upload type, size, content, and ownership.
- Apply least privilege to database, Qdrant, Ollama, and provider credentials.

## 13. Testing Standards

- Follow [TESTING_STRATEGY.md](TESTING_STRATEGY.md).
- Tests are deterministic and independent.
- Use factories/builders rather than opaque shared fixtures.
- Do not call paid or live external providers in normal unit tests.
- Financial calculations use reviewed golden fixtures and edge cases.
- AI tests assert retrieval, grounding, citation, schema, and safety behavior—not exact prose.

## 14. Documentation Standards

- Public modules and non-obvious domain rules require concise documentation.
- Explain why, assumptions, units, and edge cases; do not narrate obvious syntax.
- Keep README/setup commands executable.
- Update architecture docs and ADRs when boundaries change.
- Include Mermaid/ASCII diagrams only when they materially clarify flow.

## 15. Naming

- Names should reflect domain language: `ResearchJob`, `SourceDocument`, `HoldingSnapshot`.
- Avoid ambiguous terms such as `data`, `manager`, `helper`, or `util` when a precise name exists.
- Include units/time basis in names when ambiguity is dangerous.
- Use `asOf` for data freshness and `createdAt`/`updatedAt` for record lifecycle.
- Use decision-support terminology; avoid `tradeSignal`, `execute`, or `autoBuy` unless explicitly describing a prohibited action.

## 16. Definition of Done

Code is done when:

- behavior and acceptance criteria are met;
- relevant tests and AI evaluations pass;
- security and privacy implications are addressed;
- error, telemetry, and failure behavior are implemented;
- documentation and contracts are current;
- migrations and rollback are considered;
- no secrets or sensitive data are introduced;
- financial output preserves source, freshness, assumptions, and disclaimer requirements.

## 17. Related Documents

- [API Guidelines](API_GUIDELINES.md)
- [Security](SECURITY.md)
- [Testing Strategy](TESTING_STRATEGY.md)
- [ADR-001](ADR-001-architecture-style.md)
