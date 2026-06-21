# MarketMind AI Local Environment Architecture

**Status:** Local development foundation

## Purpose

The local environment provides repeatable stateful infrastructure for backend
and AI-service development. It uses Docker Compose to run PostgreSQL, Redis,
Qdrant, pgAdmin, Loki, Promtail, and Grafana without placing credentials or
infrastructure concerns in application source code.

This is a single-workstation development topology. Production environments require managed secrets, encrypted transport, backups, high availability, network policy, observability, and environment-specific capacity planning.

## Topology

```text
Developer workstation
├── 127.0.0.1:5432 ── PostgreSQL
├── 127.0.0.1:6379 ── Redis
├── 127.0.0.1:6333 ── Qdrant HTTP
├── 127.0.0.1:6334 ── Qdrant gRPC
├── 127.0.0.1:5050 ── pgAdmin
├── 127.0.0.1:3100 ── Loki
└── 127.0.0.1:3000 ── Grafana

marketmind-local-network
├── postgres:5432
├── redis:6379
├── qdrant:6333/6334
├── pgadmin:80
├── loki:3100
├── promtail:9080
└── grafana:3000
```

Host ports bind to loopback. Containers communicate through service DNS names on the custom bridge network.

## Startup Order

1. Docker creates the custom network and named volumes.
2. PostgreSQL, Redis, Qdrant, and Loki start independently.
3. Each service completes its health check.
4. pgAdmin starts after PostgreSQL becomes healthy; Promtail and Grafana start
   after Loki starts.
5. The Spring Boot backend starts and Flyway applies migrations.
6. AI and frontend services start when their required dependencies are ready.

`depends_on` coordinates container startup; applications must still use timeouts, bounded retries, and explicit degraded behavior.

## Data Ownership

| Service | Responsibility | Authority |
| --- | --- | --- |
| PostgreSQL | Users, companies, portfolios, transactions, documents, alerts | Canonical |
| Redis | Cache and transient coordination | Non-canonical |
| Qdrant | Document vectors and retrieval metadata | Derived and rebuildable |
| pgAdmin | Local database administration | Operator tool only |
| Loki | Local searchable log retention | Operational telemetry |
| Promtail | Local log collection | Collector only |
| Grafana | Local log exploration | Operator tool only |

Flyway in the backend owns application schema evolution. `docker/initdb` is reserved for prerequisites that must exist before Flyway runs.

## Persistence

| Volume | Contents |
| --- | --- |
| `marketmind-postgres-data` | PostgreSQL data and WAL |
| `marketmind-redis-data` | Local Redis append-only state |
| `marketmind-qdrant-data` | Qdrant collections |
| `marketmind-pgadmin-data` | pgAdmin preferences |
| `marketmind-loki-data` | Local Loki chunks and indexes |
| `marketmind-grafana-data` | Grafana preferences |
| `marketmind-promtail-positions` | Promtail file offsets |

`docker compose down` preserves volumes. `docker compose down -v` permanently deletes them.

## Networking and Ports

| Service | Container port | Default host port |
| --- | ---: | ---: |
| PostgreSQL | `5432` | `5432` |
| Redis | `6379` | `6379` |
| Qdrant HTTP | `6333` | `6333` |
| Qdrant gRPC | `6334` | `6334` |
| pgAdmin | `80` | `5050` |
| Loki | `3100` | `3100` |
| Grafana | `3000` | `3000` |

Host applications use `127.0.0.1`. Compose containers use `postgres`, `redis`, and `qdrant`. Container IP addresses must not be used as stable configuration.

## Configuration and Secrets

The committed `docker/.env.example` contains predictable local-only defaults. Developers copy it to ignored `docker/.env`.

- Never commit populated environment files.
- Never reuse local defaults in shared environments.
- Shared and production deployments must use an approved secret manager.
- Required Compose values fail fast when absent.
- Data and model services must not be publicly exposed.

## Health and Recovery

- PostgreSQL uses `pg_isready`.
- Redis uses an authenticated `PING`.
- Qdrant checks local service readiness.
- pgAdmin uses its HTTP ping endpoint.
- Grafana uses its HTTP health endpoint.
- Promtail forwards Docker socket-discovered logs and the backend rolling file
  to Loki.
- Services use `restart: unless-stopped`.
- Container logs use bounded rotation.
- CPU, memory, and process limits protect the workstation.

Restart policies improve local recovery but do not provide high availability.

## Kubernetes Mapping

| Docker Compose | Kubernetes/Helm |
| --- | --- |
| PostgreSQL container | PostgreSQL StatefulSet, Service, and PVC |
| Redis container | Optional Redis Deployment and Service |
| Qdrant container | Qdrant StatefulSet, Service, and PVC |
| pgAdmin | On-demand operator tool; not deployed by default |
| Loki, Promtail, Grafana | Production observability stack selected separately |
| Bridge service DNS | Kubernetes Service DNS |
| `docker/.env` | ConfigMap plus Secret/external secret |
| Health checks | Readiness and liveness probes |
| Resource limits | Pod requests and limits |
| Named volumes | PersistentVolumeClaims and StorageClasses |

Production Kubernetes additionally requires NetworkPolicies, Pod Security controls, TLS, managed secrets, disruption budgets, backups, restore testing, and high-availability design.

## Operational Commands

```bash
cp docker/.env.example docker/.env
docker compose --env-file docker/.env config --quiet
docker compose --env-file docker/.env up -d
docker compose --env-file docker/.env ps
docker compose --env-file docker/.env down
```

For a deliberate destructive reset:

```bash
docker compose --env-file docker/.env down -v
```

See [docker/README.md](../../docker/README.md) and [kubernetes/README.md](../../kubernetes/README.md) for detailed operating instructions.
