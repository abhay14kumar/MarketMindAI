# MarketMind AI Backend

Spring Boot foundation for the MarketMind AI public backend API.

## Technology

- Java 21
- Spring Boot 3.5.15
- Maven
- Spring Web and Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Boot Actuator

## Current Scope

This phase provides infrastructure only:

- application bootstrap;
- PostgreSQL and JPA configuration;
- initial Flyway schema;
- CORS for the Vite frontend at `http://localhost:5173`;
- global problem-details exception handling;
- application and Actuator health endpoints.

Authentication, repositories, entities, use cases, and business logic are intentionally deferred.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL

Verify the toolchain:

```bash
java -version
mvn -version
```

## Database Setup

The default backend configuration matches the local PostgreSQL service defined by `docker/.env.example`:

```text
URL:      jdbc:postgresql://127.0.0.1:5432/marketmind
Username: marketmind_user
Password: marketmind_pass
```

These predictable values are development-only. Shared and production environments must override them through environment variables or an approved secrets manager:

```bash
export DB_URL='jdbc:postgresql://database-host:5432/marketmind'
export DB_USERNAME='runtime_database_user'
read -s DB_PASSWORD
export DB_PASSWORD
```

Optional variables:

```bash
export SERVER_PORT='8080'
export CORS_ALLOWED_ORIGINS='http://localhost:5173'
export DB_MAX_POOL_SIZE='10'
export DB_MIN_IDLE='2'
export DB_CONNECTION_TIMEOUT_MS='30000'
```

`CORS_ALLOWED_ORIGINS` accepts a comma-separated list.

## Run Locally

First start the local infrastructure. From `backend/`:

```bash
docker compose --env-file ../docker/.env up -d
```

Then start Spring Boot:

```bash
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration` during startup. Hibernate validates the migrated schema and does not create or update tables.

Verify database-backed application health:

```bash
curl http://localhost:8080/actuator/health
```

## Build and Test

```bash
mvn clean verify
```

## Health Checks

Application health:

```bash
curl http://localhost:8080/api/v1/health
```

Actuator health, including configured dependency indicators:

```bash
curl http://localhost:8080/actuator/health
```

Expected application response:

```json
{
  "status": "UP",
  "service": "marketmind-backend"
}
```

## Package Structure

```text
src/main/java/com/marketmind/
├── MarketMindApplication.java
├── common/
│   └── exception/
├── config/
└── health/
    └── adapter/in/web/
```

Future features should follow clean architecture boundaries:

```text
web adapter -> application use case -> domain <- persistence/provider adapter
```

Domain and application layers must not depend on Spring MVC, JPA entities, or provider-specific implementations.

## Security

- Do not store database passwords, API keys, tokens, or other secrets in this repository.
- Use environment variables locally and an approved secret manager in shared environments.
- PostgreSQL should not be publicly exposed.
- The current backend intentionally has no authentication and is suitable only for local foundation work until access control is implemented.
