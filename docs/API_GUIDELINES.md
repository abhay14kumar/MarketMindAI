# MarketMind AI — API Guidelines

**Status:** Draft for Phase 1

**Scope:** Public frontend-to-backend APIs and private backend-to-AI-service APIs

## 1. Principles

- The Spring Boot backend is the initial public system API.
- The FastAPI AI service is private and never called directly by the browser.
- Contracts are explicit, versioned, validated, documented with OpenAPI, and backward-compatible within a major version.
- APIs expose source provenance and freshness for financial information.
- Long-running ingestion and AI workflows are asynchronous job resources.
- API responses provide decision support only and cannot place or authorize trades.
- Sensitive data is minimized; secrets and internal implementation details are never returned.

## 2. Protocol and Format

- HTTPS is mandatory outside isolated local development.
- JSON is the default representation.
- UTF-8 is required.
- Public routes begin with `/api/v1`.
- Private routes begin with `/internal/v1`.
- Resource names are lowercase plural nouns with hyphens only when needed.
- OpenAPI is the contract of record for HTTP interfaces.

Examples:

```text
GET  /api/v1/companies/{companyId}
GET  /api/v1/companies/{companyId}/documents
POST /api/v1/portfolios
POST /api/v1/research-jobs
GET  /api/v1/research-jobs/{jobId}
```

Avoid action-heavy RPC paths. Use actions only when they do not map cleanly to resources, for example:

```text
POST /api/v1/ingestion-jobs/{jobId}/retry
POST /api/v1/research-jobs/{jobId}/cancel
```

## 3. Resource Modeling

- Use stable opaque IDs; do not expose database sequence assumptions.
- Return resource links or IDs needed for navigation, not internal table details.
- Embed small, stable child objects only when it prevents unnecessary calls.
- Paginate collections.
- Keep write models separate from read models where validation or sensitive fields differ.
- Do not expose persistence entities directly.

## 4. HTTP Methods and Status Codes

| Method | Use |
| --- | --- |
| `GET` | Read without side effects |
| `POST` | Create resources or submit jobs |
| `PUT` | Full replacement only when supported |
| `PATCH` | Partial update with explicit allowed fields |
| `DELETE` | Delete or request deletion according to retention policy |

Common status codes:

| Code | Meaning |
| --- | --- |
| `200 OK` | Successful read/update |
| `201 Created` | Resource created; include `Location` |
| `202 Accepted` | Asynchronous job accepted |
| `204 No Content` | Successful operation with no body |
| `400 Bad Request` | Malformed syntax or unsupported request |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated but not permitted |
| `404 Not Found` | Resource absent or intentionally concealed |
| `409 Conflict` | State/version/idempotency conflict |
| `422 Unprocessable Content` | Semantically invalid fields |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Unexpected server error |
| `502/503/504` | Upstream failure, unavailable service, or timeout |

## 5. Naming and Types

- JSON fields use `camelCase`.
- Java and Python internal naming follow their language standards.
- Timestamps use ISO 8601 UTC, for example `2026-06-18T16:30:00Z`.
- Dates use `YYYY-MM-DD`.
- Money uses an object:

```json
{
  "amount": "1250.50",
  "currency": "INR"
}
```

- Decimal values are serialized without binary floating-point assumptions.
- Percentages state their convention in the schema.
- Enums use stable uppercase values, for example `COMPLETED`.
- Nullable, optional, absent, and empty are distinct and documented.

## 6. Financial Data Requirements

Financial and market-data responses include:

- `source`;
- `asOf`;
- unit and currency;
- reporting period or observation timestamp;
- correction/provisional status where relevant.

Calculated values include:

- formula or calculation version;
- input `asOf`;
- missing-data indicators;
- assumptions where the result is scenario-based.

The API must not describe delayed data as real-time. The term “live” may be used only when a contractual and technical freshness definition exists.

## 7. Asynchronous Jobs

Ingestion and research submission returns `202 Accepted`:

```json
{
  "id": "job-id",
  "type": "RESEARCH",
  "status": "QUEUED",
  "submittedAt": "2026-06-18T16:30:00Z",
  "statusUrl": "/api/v1/research-jobs/job-id"
}
```

Job resources include:

- stable status;
- progress stage, not invented percentage;
- created/started/completed timestamps;
- safe error code and message;
- result link when completed;
- cancellation availability.

Polling is the baseline. Server-sent events may be added for progress or token streaming after authorization and reconnection behavior are designed.

## 8. Pagination, Filtering, and Sorting

- Cursor pagination is preferred for changing or large datasets.
- Offset pagination may be used for small administrative lists.
- Default and maximum page sizes are enforced.
- Filters are allowlisted and documented.
- Sort fields are allowlisted; stable secondary ordering is required.

Example:

```text
GET /api/v1/companies/{id}/documents?documentType=ANNUAL_REPORT&limit=25&cursor=...
```

## 9. Idempotency and Concurrency

- Retriable `POST` operations accept `Idempotency-Key`.
- The server scopes keys to the caller and endpoint and retains them for a documented period.
- Reusing a key with a different payload returns `409 Conflict`.
- Mutable resources use version fields/ETags or equivalent optimistic concurrency.
- Duplicate asynchronous delivery is expected; handlers remain idempotent.

## 10. Error Contract

Use a problem-details-compatible response:

```json
{
  "type": "https://docs.marketmind.local/problems/validation-failed",
  "title": "Request validation failed",
  "status": 422,
  "code": "VALIDATION_FAILED",
  "legacyCode": "VALIDATION_ERROR",
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/portfolios",
  "correlationId": "correlation-id",
  "requestId": "correlation-id",
  "timestamp": "2026-06-21T07:00:00Z",
  "fieldErrors": [
    {
      "field": "baseCurrency",
      "code": "UNSUPPORTED_CURRENCY",
      "message": "The currency is not currently supported."
    }
  ]
}
```

Rules:

- Error codes are stable and machine-readable.
- `correlationId` and `requestId` currently identify the same request and are
  both returned for client and infrastructure interoperability.
- Validation failures return field-level `fieldErrors`; the legacy `errors`
  property is temporarily retained as a compatibility alias. Request-wide
  failures may use `_request` as the field.
- Malformed JSON, unsupported enum values, missing multipart parts, upload
  limits, external providers, Qdrant, and Ollama use distinct error codes.
- Messages are safe for users and do not include stack traces, queries, prompts, secrets, or provider credentials.
- Authorization failures do not reveal whether another user's resource exists.
- Internal upstream details are logged safely and mapped to a controlled public error.

Stable domain codes currently include:

| Code | Meaning |
|---|---|
| `INVALID_REQUEST` | Malformed input, missing parameters, or unsupported upload |
| `VALIDATION_FAILED` | One or more semantic or field constraints failed |
| `INVALID_ENUM_VALUE` | Enum input is unsupported; allowed values are returned |
| `RESOURCE_NOT_FOUND` | Requested resource does not exist |
| `DUPLICATE_RESOURCE` | A unique resource or equivalent record already exists |
| `EXTERNAL_SERVICE_FAILURE` | A remote data provider failed |
| `QDRANT_FAILURE` | Vector storage or retrieval failed |
| `OLLAMA_FAILURE` | Local model inference or embedding failed |
| `DOCUMENT_DOWNLOAD_FAILED` | Document acquisition failed |
| `DOCUMENT_EXTRACTION_FAILED` | Text extraction failed |
| `EMBEDDING_FAILED` | Document or query embedding failed |

Legacy internal codes may temporarily appear as `legacyCode` during migration.

## 11. Authentication and Authorization

- Authentication uses the selected standards-based identity provider; the decision is pending.
- Browser sessions should prefer secure, `HttpOnly`, `SameSite` cookies where architecture permits.
- Every user-owned resource is authorized in the application/service layer.
- Service-to-service APIs require dedicated workload identity or short-lived credentials.
- Internal headers are not accepted as proof of user identity from public clients.
- Administrative endpoints require explicit roles and audit logging.

## 12. Research Response Contract

A completed research report should expose:

- summary;
- findings;
- facts, calculations, assumptions, and interpretation as distinct fields;
- risks and counter-thesis;
- citations;
- data `asOf`;
- confidence/uncertainty;
- optional decision-support label;
- disclaimer and disclaimer version;
- model/policy metadata only to the degree safe and useful.

It must not expose hidden chain-of-thought. Concise structured rationale and evidence are sufficient.

## 13. Versioning and Compatibility

- Major breaking changes use a new path version.
- Additive optional fields are normally backward-compatible.
- Removing/renaming fields, changing meaning, narrowing enums, or changing units is breaking.
- Deprecated fields include a migration path and removal date.
- Provider-specific models do not leak into stable public contracts.
- OpenAPI compatibility checks run in CI once implementation begins.

## 14. Rate Limits and Work Budgets

- Apply caller- and endpoint-specific limits.
- Expensive AI endpoints also enforce document, context, agent, runtime, and concurrency budgets.
- Return `429` with a retry hint where safe.
- Never bypass authorization or quality validation to satisfy latency.

## 15. Logging and Observability

- Accept or generate a correlation ID; do not trust arbitrary client values without validation.
- Accept `X-Correlation-Id` and `X-Request-ID`; return both headers using the
  canonical correlation value.
- Add the canonical ID to MDC as `correlationId` and `requestId`, and clear MDC
  after every request to prevent thread-pool leakage.
- Keep local logs human-readable with correlation ID, method, path, status,
  and duration. A structured encoder may be enabled later without changing
  application log calls.
- Propagate trace context to internal calls.
- Log route templates rather than sensitive URL values where practical.
- Do not log authorization headers, cookies, secrets, full prompts, raw portfolio data, or full documents by default.
- Record latency, outcome, stable error code, and job identifiers.

## 16. API Review Checklist

- Is the resource owner and authorization rule explicit?
- Are freshness, source, unit, and currency represented?
- Is the operation idempotent or protected against retries?
- Does a long operation return a job?
- Are errors stable and non-sensitive?
- Is pagination bounded?
- Is the OpenAPI schema complete?
- Does the response avoid automatic-trading semantics?
- Are disclaimer and citation requirements satisfied for research output?

## 17. Related Documents

- [High-Level Design](HLD.md)
- [Low-Level Design](LLD.md)
- [Security](SECURITY.md)
- [Coding Standards](CODING_STANDARDS.md)
