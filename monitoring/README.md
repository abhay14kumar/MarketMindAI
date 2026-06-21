# MarketMind AI Local Central Logging

The local logging stack uses Grafana, Loki, and Promtail to search:

- Docker Compose container logs;
- the Spring Boot backend rolling log;
- correlation IDs shared by API responses and backend logs.

This is workstation-only infrastructure. It does not provide cloud monitoring,
multi-tenancy, TLS, alerting, or production retention guarantees.

## Components

| Component | Purpose | Local endpoint |
|---|---|---|
| Grafana | Log exploration UI | `http://localhost:3000` |
| Loki | Local log storage and query API | `http://127.0.0.1:3100` |
| Promtail | Docker and backend log collector | Compose network only |

Loki is provisioned automatically as Grafana's default datasource.

## Configure

Create the ignored local environment file:

```bash
cp docker/.env.example docker/.env
```

Default local Grafana settings:

```text
URL:      http://localhost:3000
Username: admin
Password: admin123
```

These credentials are development-only and must not be used in shared or
production environments.

## Start

From the repository root:

```bash
docker compose --env-file docker/.env config --quiet
docker compose --env-file docker/.env up -d
docker compose --env-file docker/.env ps
```

Start only the logging stack when the data services are already running:

```bash
docker compose --env-file docker/.env up -d loki promtail grafana
```

Inspect startup logs:

```bash
docker compose --env-file docker/.env logs --tail=100 loki promtail grafana
```

Verify Loki:

```bash
curl --fail http://127.0.0.1:3100/ready
```

Verify Grafana:

```bash
curl --fail http://127.0.0.1:3000/api/health
```

## Start the Backend

Promtail mounts `backend/logs/` read-only. Run Spring Boot with the local
profile so Logback writes `backend/logs/marketmind-backend.log`:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

The file uses bounded daily and size-based rotation. The directory and all
`*.log` files are ignored by Git.

Send a traceable request:

```bash
curl -i \
  -H 'X-Correlation-Id: local-observability-001' \
  http://localhost:8080/actuator/health
```

## Search Logs

Open Grafana, select **Explore**, and use the provisioned **Loki** datasource.

PostgreSQL container:

```logql
{container_name="marketmind-postgres"}
```

All backend file logs:

```logql
{job="marketmind-backend"}
```

Backend logs containing correlation context:

```logql
{job="marketmind-backend"} |= "correlationId"
```

One request:

```logql
{job="marketmind-backend"} |= "local-observability-001"
```

All Compose container errors:

```logql
{job="docker"} |= "ERROR"
```

## Collection Model

Promtail uses Docker service discovery through the read-only Docker socket.
Container names are normalized into the `container_name` label. Spring Boot
logs are collected separately with `job="marketmind-backend"`.

Docker socket access is powerful even when mounted read-only. It is acceptable
only for this isolated local developer stack. Do not copy this design into
shared or production environments without a security review.

## Persistence and Retention

- Loki data: `marketmind-loki-data`
- Grafana data: `marketmind-grafana-data`
- Promtail positions: `marketmind-promtail-positions`
- Loki retention: seven days
- Backend rolling-file history: seven days, bounded to 250 MB

Stopping containers preserves logs:

```bash
docker compose --env-file docker/.env down
```

Deleting all volumes also removes Loki and Grafana state:

```bash
docker compose --env-file docker/.env down -v
```

## Troubleshooting

If backend logs do not appear:

1. confirm `backend/logs/marketmind-backend.log` exists;
2. confirm the backend was started with the `local` profile;
3. inspect Promtail logs;
4. verify the mounted path:

   ```bash
   docker compose --env-file docker/.env exec promtail \
     ls -la /var/log/marketmind/backend
   ```

If Docker logs do not appear, verify Docker socket access:

```bash
docker compose --env-file docker/.env exec promtail \
  ls -l /var/run/docker.sock
```

Promtail is retained here because this sprint explicitly targets it. New
production designs should evaluate Grafana Alloy or another actively supported
collector before deployment.
