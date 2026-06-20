# MarketMind AI Local Infrastructure

This directory supports the repository-root [`docker-compose.yml`](../docker-compose.yml), which provides the local data infrastructure for MarketMind AI.

## Services

| Service | Image | Container port | Default host endpoint | Purpose |
| --- | --- | ---: | --- | --- |
| PostgreSQL | `postgres:16-alpine` | `5432` | `127.0.0.1:5432` | Canonical relational database |
| Redis | `redis:7-alpine` | `6379` | `127.0.0.1:6379` | Cache and future job coordination |
| Qdrant | `qdrant/qdrant:latest` | `6333`, `6334` | `127.0.0.1:6333`, `127.0.0.1:6334` | Vector index |
| pgAdmin | `dpage/pgadmin4:latest` | `80` | `http://127.0.0.1:5050` | Local PostgreSQL administration |

All host bindings use loopback by default. Other containers on the Compose network use service DNS names such as `postgres`, `redis`, and `qdrant`.

## Prerequisites

- Docker Engine or Docker Desktop
- Docker Compose v2
- At least 4 GB of memory available to Docker for comfortable operation

Verify the client:

```bash
docker version
docker compose version
```

## Configure

Create the untracked local environment file from the complete development template:

```bash
cp docker/.env.example docker/.env
```

The committed values are predictable local-development credentials. They are not real secrets and must not be reused in shared, staging, or production environments. If stronger workstation-local credentials are wanted, generate them before the first startup:

```bash
openssl rand -base64 32
```

Do not commit `docker/.env`. It is ignored by both the repository-root `.gitignore` and [`docker/.gitignore`](.gitignore).

Compose variable interpolation is explicitly driven by this file:

```bash
docker compose --env-file docker/.env config --quiet
```

The command must succeed before starting the stack.

## Reset Old Credentials and Start

The official PostgreSQL image applies `POSTGRES_USER` and `POSTGRES_PASSWORD` only when it initializes a new data directory. If the named volume was created with old credentials, changing `docker/.env` does not change the existing database role password.

For the current local credential reset, remove the old containers and volumes:

```bash
docker compose --env-file docker/.env down -v
```

> This deletes all local PostgreSQL, Redis, Qdrant, and pgAdmin data. Do not run it when local data must be preserved; update the database role password explicitly or back up the data first.

Start a clean environment:

```bash
docker compose --env-file docker/.env up -d
```

Compose starts PostgreSQL, Redis, and Qdrant independently. pgAdmin starts after PostgreSQL reports healthy.

Check status:

```bash
docker ps
docker compose --env-file docker/.env ps
docker compose --env-file docker/.env logs --tail=100
```

Wait until each service reports `healthy` before starting application services.

## Health Verification

PostgreSQL:

```bash
docker compose --env-file docker/.env exec postgres \
  sh -ec 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Connect from a host with the PostgreSQL client installed:

```bash
psql -h 127.0.0.1 -p 5432 -U marketmind_user -d marketmind
```

When prompted, enter `marketmind_pass`.

Redis:

```bash
docker compose --env-file docker/.env exec redis \
  sh -ec 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
```

Qdrant:

```bash
curl -H "api-key: marketmind_qdrant_key" http://localhost:6333/collections
```

pgAdmin:

```bash
curl --fail http://127.0.0.1:5050/misc/ping
```

If a host port was changed in `docker/.env`, use that value instead.

## Application Connection Settings

Applications running directly on the host:

```text
PostgreSQL: jdbc:postgresql://127.0.0.1:5432/marketmind
Redis:      redis://:<REDIS_PASSWORD>@127.0.0.1:6379/0
Qdrant:     http://127.0.0.1:6333
```

Applications attached to `marketmind-local-network`:

```text
PostgreSQL: jdbc:postgresql://postgres:5432/marketmind
Redis:      redis://:<REDIS_PASSWORD>@redis:6379/0
Qdrant:     http://qdrant:6333
```

Qdrant clients must send the configured API key. Credentials should enter application processes through environment variables or a secret manager, never source code.

## pgAdmin

Open:

```text
http://localhost:5050
```

Local login:

```text
Email:    admin@marketmind.local
Password: admin123
```

These credentials are intentionally local and must not be used outside an isolated developer workstation.

Register the PostgreSQL server:

| Field | Value |
| --- | --- |
| Host name/address | `postgres` |
| Port | `5432` |
| Maintenance database | value of `POSTGRES_DB` |
| Username | value of `POSTGRES_USER` |
| Password | value of `POSTGRES_PASSWORD` |

Use `postgres`, not `localhost`: pgAdmin runs inside the Compose network.

## Persistence

Named volumes:

- `marketmind-postgres-data`
- `marketmind-redis-data`
- `marketmind-qdrant-data`
- `marketmind-pgadmin-data`

List them:

```bash
docker volume ls --filter name=marketmind-
```

Stopping containers does not delete data:

```bash
docker compose --env-file docker/.env down
```

Delete containers and all local data only when explicitly intended:

```bash
docker compose --env-file docker/.env down -v
```

This operation is destructive and cannot be undone without a backup.

## PostgreSQL Initialization and Migrations

Files under [`initdb/`](initdb/) run only when PostgreSQL initializes an empty volume.

Application tables are managed by backend Flyway migrations. Do not copy Flyway SQL into `initdb/`; doing so creates two schema owners and unpredictable upgrade behavior.

If initialization files change after the volume exists, they will not run automatically. Apply an explicit migration or recreate the local volume only when data loss is acceptable.

## Logs

All services use Docker's `json-file` logging driver with bounded rotation:

- maximum file size: `LOG_MAX_SIZE`;
- retained files: `LOG_MAX_FILES`.

View service logs:

```bash
docker compose --env-file docker/.env logs -f postgres
docker compose --env-file docker/.env logs -f redis
docker compose --env-file docker/.env logs -f qdrant
docker compose --env-file docker/.env logs -f pgadmin
```

PostgreSQL writes operational logs to standard error so Docker rotation applies.

## Resource Controls

CPU and memory limits are configured per service in `docker/.env.example`. Tune them for the workstation without removing safeguards.

Current defaults reserve approximately:

- PostgreSQL: 1 CPU / 1 GiB limit
- Redis: 0.5 CPU / 512 MiB limit
- Qdrant: 1 CPU / 1 GiB limit
- pgAdmin: 0.5 CPU / 512 MiB limit

Resource limits protect the workstation; they are not production sizing recommendations.

## Upgrade Procedure

1. Back up important local data.
2. Change the image tag in `docker/.env`.
3. Review upstream release and migration notes.
4. Pull and recreate:

   ```bash
   docker compose --env-file docker/.env pull
   docker compose --env-file docker/.env up -d
   ```

5. Verify health and application migrations.

`latest` is convenient for local Qdrant and pgAdmin, but reproducible CI and shared environments should pin tested versions or immutable image digests.

## Troubleshooting

### Port conflict

Change the corresponding `POSTGRES_PORT`, `REDIS_PORT`, `QDRANT_PORT`, `QDRANT_GRPC_PORT`, or `PGADMIN_PORT` value in `docker/.env`, then recreate the service.

### Service remains unhealthy

```bash
docker compose --env-file docker/.env ps
docker compose --env-file docker/.env logs --tail=200 <service>
docker inspect --format '{{json .State.Health}}' marketmind-<service>
```

### PostgreSQL initialization did not rerun

Initialization scripts run only against a new volume. Prefer Flyway for schema evolution. If local data can be destroyed:

```bash
docker compose --env-file docker/.env down -v
docker compose --env-file docker/.env up -d
```

### Reset one service

Remove only that service's container. Preserve or remove its named volume deliberately:

```bash
docker compose --env-file docker/.env rm -sf redis
docker compose --env-file docker/.env up -d redis
```

## Security Boundaries

- Host ports bind to `127.0.0.1`; do not change to `0.0.0.0` without a reviewed need.
- PostgreSQL, Redis, Qdrant, and pgAdmin require local credentials.
- This environment does not provide TLS. It is for an isolated developer workstation.
- Do not use these credentials or images as-is in shared or production environments.
- Do not place production data in local volumes.
- Rotate any credential exposed in logs, shell history, tickets, or source control.

See [`../architecture/deployment/local-environment.md`](../architecture/deployment/local-environment.md) for the deployment model and Kubernetes mapping.
