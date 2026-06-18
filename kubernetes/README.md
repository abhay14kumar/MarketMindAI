# MarketMind AI Kubernetes Foundation

This directory documents the local Kubernetes deployment workflow. The Helm chart is located at [`../helm/marketmind-ai`](../helm/marketmind-ai).

## Scope

The chart deploys:

- MarketMind backend
- MarketMind frontend
- MarketMind AI service
- PostgreSQL with persistent storage
- Qdrant with persistent storage
- Optional Redis
- Optional Ingress

Ollama is not deployed by this chart. The AI service connects to the configurable `aiService.env.ollamaBaseUrl`. For local clusters, make sure that address is reachable from inside the cluster.

This is a development foundation, not a production-ready topology. Before production use, add managed secrets, network policies, pod security controls, backup/restore procedures, high availability, TLS, autoscaling, and environment-specific observability.

## Prerequisites

- Kubernetes 1.28+
- Helm 3
- `kubectl`
- A local cluster such as Docker Desktop Kubernetes, kind, or minikube
- Locally built application images, or images pushed to a registry

## Local Images

The default chart values use:

```text
marketmind/backend:local
marketmind/frontend:local
marketmind/ai-service:local
```

Build these images when each service has a Dockerfile:

```bash
docker build -t marketmind/backend:local backend/
docker build -t marketmind/frontend:local frontend/
docker build -t marketmind/ai-service:local ai-service/
```

Make the images available to your local cluster:

```bash
# kind
kind load docker-image \
  marketmind/backend:local \
  marketmind/frontend:local \
  marketmind/ai-service:local

# minikube
minikube image load marketmind/backend:local
minikube image load marketmind/frontend:local
minikube image load marketmind/ai-service:local
```

Docker Desktop Kubernetes can normally use images from the local Docker image store. If your cluster uses a separate runtime, load or push the images explicitly.

### Registry images

For a shared cluster, replace the local repositories and tags with registry images:

```bash
helm upgrade --install marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --create-namespace \
  --set backend.image.repository=ghcr.io/your-org/marketmind-backend \
  --set backend.image.tag=0.1.0 \
  --set frontend.image.repository=ghcr.io/your-org/marketmind-frontend \
  --set frontend.image.tag=0.1.0 \
  --set aiService.image.repository=ghcr.io/your-org/marketmind-ai-service \
  --set aiService.image.tag=0.1.0
```

Configure `imagePullSecrets` in an uncommitted values file when the registry is private.

## Secrets

The chart contains no real credentials. Secret values are empty by default.

For local development, create a Kubernetes Secret before installation:

```bash
kubectl create namespace marketmind

kubectl -n marketmind create secret generic marketmind-local-db \
  --from-literal=postgres-username='marketmind_local' \
  --from-literal=postgres-password='replace-with-a-local-only-password'
```

Then install with:

```bash
helm upgrade --install marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db
```

The existing Secret must provide these keys:

- `postgres-username`
- `postgres-password`

Do not commit Secret manifests, decrypted secret files, command history containing real credentials, or populated values files. Use an approved secret manager and External Secrets/CSI integration for shared environments.

## Install

From the repository root:

```bash
helm lint ./helm/marketmind-ai

helm upgrade --install marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --create-namespace \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db
```

Inspect the deployment:

```bash
kubectl -n marketmind get pods,services,pvc
kubectl -n marketmind get events --sort-by=.lastTimestamp
```

## Access Without Ingress

Port-forward the frontend and backend:

```bash
kubectl -n marketmind port-forward service/marketmind-marketmind-ai-frontend 5173:80
kubectl -n marketmind port-forward service/marketmind-marketmind-ai-backend 8080:8080
```

Open `http://localhost:5173` and check backend health at:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

The generated service prefix depends on the Helm release name and any name overrides. Use `kubectl -n marketmind get services` if you install with a different release name.

## Optional Redis

Redis is disabled by default. Enable it for local workflows that need caching or job coordination:

```bash
helm upgrade --install marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db \
  --set redis.enabled=true
```

The chart injects `REDIS_URL` into the AI service when Redis is enabled. The Redis deployment is intentionally ephemeral in this foundation.

## Optional Ingress

Ingress is disabled by default. Install an ingress controller first, then enable it:

```bash
helm upgrade --install marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db \
  --set ingress.enabled=true \
  --set ingress.className=nginx \
  --set ingress.host=marketmind.local
```

Map `marketmind.local` to the local ingress address and configure TLS before using this pattern outside isolated development.

The default routes are:

- `/` → frontend
- `/api` → backend

The AI service, PostgreSQL, Qdrant, and Redis are not exposed through Ingress.

## Persistence

PostgreSQL and Qdrant use StatefulSet volume claim templates:

```yaml
postgres:
  persistence:
    enabled: true
    size: 5Gi
    storageClass: ""

qdrant:
  persistence:
    enabled: true
    size: 5Gi
    storageClass: ""
```

An empty `storageClass` uses the cluster default. Set it explicitly when the cluster has no default provisioner.

Disabling persistence switches that workload to `emptyDir`; all data is lost when the Pod is replaced:

```bash
--set postgres.persistence.enabled=false \
--set qdrant.persistence.enabled=false
```

## Ollama Connectivity

The default AI-service URL is:

```yaml
aiService:
  env:
    ollamaBaseUrl: http://host.docker.internal:11434
```

This commonly works with Docker Desktop. For kind, minikube, or a remote cluster, replace it with an address reachable from cluster Pods:

```bash
--set aiService.env.ollamaBaseUrl=http://your-ollama-host:11434
```

Do not expose Ollama publicly without authentication, network policy, and transport security.

## Configuration Notes

- Values for images, ports, replicas, resources, probes, storage, and environment settings live in `values.yaml`.
- Backend configuration matches the environment variables in `backend/src/main/resources/application.yml`.
- Vite variables are normally compiled into frontend assets at build time. The frontend image must implement runtime configuration if `VITE_API_BASE_URL` is expected to change after the image is built.
- The backend and AI service should keep their health paths aligned with the chart values.
- Local placeholder images do not exist until the services are containerized and built.

## Validate Changes

```bash
helm lint ./helm/marketmind-ai
helm template marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db
```

Also validate optional resources:

```bash
helm template marketmind ./helm/marketmind-ai \
  --namespace marketmind \
  --set secrets.create=false \
  --set secrets.existingSecret=marketmind-local-db \
  --set redis.enabled=true \
  --set ingress.enabled=true
```

## Uninstall

```bash
helm uninstall marketmind --namespace marketmind
```

StatefulSet PVCs can remain after uninstall. Review them before deletion:

```bash
kubectl -n marketmind get pvc
```

Deleting PVCs permanently removes local PostgreSQL and Qdrant data.
