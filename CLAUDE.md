# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Lite Bank Demo is a microservices banking system with 11 services (10 custom + API Gateway) demonstrating coordination layer architecture, pessimistic-lock-based distributed transactions, and complete OpenTelemetry observability.

## Response
請用繁體中文回答

## Build & Development Commands

**重要規則：禁止在本地使用 `mvn` 指令。所有編譯和測試必須透過 Docker Compose 或在容器內執行。**

```bash
# Start all infrastructure (PostgreSQL, Kafka, Grafana stack, OTel Collector)
docker compose up -d

# Build and restart a single service (recommended)
docker compose up -d --build <service-name>

# Rebuild all services
docker compose up -d --build

# Build a single service inside container (if needed)
docker compose exec <service-name> mvn clean package -DskipTests

# Run tests inside container
docker compose exec <service-name> mvn test

# Run a single E2E test class inside container
docker compose exec api-gateway mvn test -Dtest="com.litebank.gateway.e2e.TransferE2ETest"

# View logs
docker compose logs -f <service-name>

# Frontend development
cd services/dashboard && npm install && ./start-dev.sh
```

## Architecture

### Coordination Layer Pattern
- **Data Layer Services (passive):** Query and validate, never modify balances directly
  - `user-service` (8080): Authentication, JWT generation
  - `account-service` (8081): Account queries (READ-ONLY)
  - `transaction-service` (8082): **ONLY service that modifies account balances**
  - `exchange-rate-service` (8084): Currency rates (stateless)

- **Coordination Layer Services (active):** Orchestrate workflows by calling data layer
  - `teller-service` (8083): Transfer / deposit / withdrawal orchestration
    (⚠️ there is NO separate `transfer-service` or `deposit-withdrawal-service` — they were merged into teller-service)
  - `exchange-service` (8085): Currency exchange orchestration

- **Analytics Services:** Event-driven analytics pipeline
  - `analytics-processor-service` (8087): Kafka consumer, processes transaction events
  - `analytics-query-service` (8088): REST API for analytics queries

- **Notification Service:** Real-time push notifications
  - `notification-service` (8089): Kafka consumer, SSE-based real-time notifications

- **API Gateway** (9000): Spring Cloud Gateway with JWT validation, uses OpenTelemetry Java Agent

### Key Rule
**All balance modifications MUST go through Transaction Service.** Coordination services orchestrate by calling data layer services; they never write directly to the database.

### OpenTelemetry Instrumentation
- Manual SDK instrumentation (version 1.44.1) in all services
- Each service's `OpenTelemetryConfig` builds an `OpenTelemetrySdk` with **TracerProvider + LoggerProvider only — NO MeterProvider**. Services emit **traces and logs, never OTel metrics**.
- W3C TraceContext propagation via `TracingFilter` in each service
- API Gateway uses Java Agent for auto-instrumentation
- In K8s the OTLP target is **Grafana Alloy** (`alloy.alloy:4318`); the chart's `otel-collector` is disabled (`observability.otelCollector.enabled=false`). Traces → Tempo, logs → Loki.

### Metrics & Monitoring (IMPORTANT — read before "adding metrics")
**All Prometheus metrics for lite-bank are derived by Alloy's spanmetrics connector from traces. Services do NOT expose any pull-based metrics — the metric pipeline is push-based and is NOT broken.**
- Backend is **rancher-monitoring-prometheus** (Prometheus Operator, `cattle-monitoring-system`); the chart's own Prometheus is disabled (`observability.prometheus.enabled=false`). Metrics arrive via Alloy **remote_write (push), NOT scrape**.
- Series under `service_namespace="lite-bank"`: `litebank_calls_total`, `litebank_duration_milliseconds_*` (spanmetrics, custom namespace `litebank`), plus span-derived `http_server_/http_client_/db_client_/rpc_/messaging_*`. Job label format `lite-bank/<service>`.
- The 4 dashboards in `grafana/dashboards/*.json` consume **only** `litebank_*` spanmetrics.
- Pod CPU/mem/restart come from rancher cAdvisor + kube-state-metrics (`container_*`, `kube_pod_*`). No JVM metrics (would require micrometer; intentionally absent).
- **Do NOT add `micrometer-registry-prometheus`, `prometheus.io/scrape` annotations, or ServiceMonitor/PodMonitor to "fix missing metrics".** They are redundant (nobody scrapes `/actuator/prometheus`) and create a parallel unused metric set. To see what exists, query rancher Prometheus PromQL or the rancher Grafana (admin/openstack) **first**.

### Frontend (React Dashboard)
- Located in `services/dashboard/`
- React 19 + Vite 7 + Tailwind CSS 4.x
- i18next for internationalization (zh-TW, en)
- Pages: Login, Dashboard, Accounts, Transfer, Exchange, History

### Database
PostgreSQL 16 with Flyway migrations in `db/migration/`:
- `users`, `accounts`, `transactions` (append-only ledger), `recipients`
- Demo users: alice, bob, charlie (password: `password123`)

## Service Communication

Services call each other via HTTP using container DNS names:
- `http://account-service:8081`
- `http://transaction-service:8082`
- etc.

JWT tokens are validated at API Gateway; downstream services receive `X-User-ID` header.

## Access Points (Local Development)

| Service | URL |
|---------|-----|
| Frontend Dashboard | http://localhost:3001 |
| API Gateway | http://localhost:9000 |
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
| PostgreSQL | localhost:5432 (litebank_user/litebank_pass) |
| Kafka (from host) | localhost:29092 |

## Technology Stack

- Java 21, Spring Boot 3.4.1, Spring Cloud Gateway 2024.0.0
- React 19 + Vite 7 + Tailwind CSS 4.x, i18next
- PostgreSQL 16 + Flyway, Kafka (KRaft mode)
- OpenTelemetry SDK 1.44.1, Grafana Tempo/Loki/Prometheus
- JUnit 5, Testcontainers, REST Assured for E2E tests
