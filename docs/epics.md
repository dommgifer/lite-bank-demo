# lite-bank-demo - Epic Breakdown

**Author:** 主人
**Date:** 2025-12-15
**Project Level:** Advanced
**Target Scale:** Workshop Demo + Technical Reference

---

## Overview

This document provides the complete epic and story breakdown for lite-bank-demo, decomposing the requirements from the [PRD](./prd.md) into implementable stories.

**Living Document Notice:** This document incorporates complete technical context from Architecture.md for implementation-ready stories.

---

## Functional Requirements Inventory

**總計：64 條功能需求（FR1-FR71，FR59-65 移至 Phase 2）**

### Category 1: Banking Operations (8 FRs)
- **FR1:** 查詢所有帳戶資訊
- **FR2:** 存款至指定帳戶
- **FR3:** 從指定帳戶提款
- **FR4:** 同幣別帳戶轉帳
- **FR5:** 跨幣別換匯轉帳
- **FR6:** 查詢即時匯率
- **FR7:** SAGA 補償邏輯自動執行
- **FR8:** 交易冪等性保證

### Category 2: Transaction & History Management (7 FRs)
- **FR9:** 查詢交易歷史記錄
- **FR10:** 依日期範圍過濾交易
- **FR11:** 記錄完整交易資訊
- **FR12:** Append-Only Transaction Table
- **FR13:** 記錄交易狀態變更
- **FR14:** SAGA 補償記錄存於 metadata
- **FR15:** 交易記錄永久儲存 trace_id

### Category 3: OpenTelemetry Instrumentation (11 FRs)
- **FR16:** 配置 Sampling 策略（Phase 1: 100%）
- **FR17:** 生成唯一 trace_id 並傳播
- **FR18:** 建立 span 記錄執行時間與狀態
- **FR19:** Span attributes 包含業務語義
- **FR20:** 正確設定 span 命名與父子層級
- **FR21:** 建立補償 span
- **FR22:** Log 包含 trace_id 與 span_id
- **FR23:** Kafka headers 傳播 W3C TraceContext
- **FR24:** 跨語言 trace propagation 無斷鏈
- **FR25:** API Response headers 包含 X-Trace-Id
- **FR26:** API Error response body 包含 trace_id

### Category 4: Grafana Stack Integration (7 FRs)
- **FR27:** 匯出 traces 至 Grafana Tempo
- **FR28:** 匯出 logs 至 Grafana Loki
- **FR29:** 匯出 metrics 至 Prometheus
- **FR30:** 配置 Tempo ↔ Loki 資料源關聯
- **FR31:** 配置 Prometheus exemplars 跳轉 Tempo
- **FR32:** 配置 Grafana Tempo 保留期限（7 天）
- **FR33:** 配置 Grafana Loki 保留期限（14 天）

### Category 5: Observability Demonstration Capabilities (7 FRs)
- **FR34:** 透過 trace_id 查詢完整 trace
- **FR35:** Tempo 跳轉 Loki logs
- **FR36:** Loki 跳轉 Tempo trace
- **FR37:** Prometheus exemplar 跳轉 Tempo
- **FR38:** Tempo 視覺化 SAGA 流程
- **FR39:** Tempo 視覺化 SAGA 補償邏輯
- **FR40:** Tempo 快速定位失敗點

### Category 6: Chaos Engineering (6 FRs)
- **FR41:** 透過 Chaos Mesh 注入網路延遲
- **FR42:** 透過 Chaos Mesh 注入 Pod 終止
- **FR43:** 透過 Chaos Mesh 注入 CPU/Memory 壓力
- **FR44:** Chaos 故障影響立即反映於 Tempo
- **FR45:** Chaos 故障可控且可恢復
- **FR46:** 透過 Tempo 在 1-2 分鐘內定位根因

### Category 7: System Deployment & Operations (7 FRs)
- **FR47:** 透過 Helm Charts 一鍵部署
- **FR48:** 透過 Docker Compose 快速啟動
- **FR49:** 配置所有微服務的 Liveness Probes
- **FR50:** 配置所有微服務的 Readiness Probes
- **FR51:** 透過 Helm values.yaml 管理環境配置
- **FR52:** 提供基礎 README.md 文件
- **FR53:** 提供基礎 DEPLOYMENT.md 文件

### Category 8: Authentication & Security (5 FRs)
- **FR54:** 使用者登入取得 JWT token
- **FR55:** API Gateway 層級驗證 JWT token
- **FR56:** JWT token 包含使用者與帳戶資訊
- **FR57:** 使用 bcrypt 或 Argon2 雜湊密碼
- **FR58:** 透過 Kubernetes Ingress 配置 HTTPS/TLS

### Category 9: API Contract & Testing (6 FRs)
- **FR66:** 統一 API Response 格式
- **FR67:** 統一 API Error Response 格式
- **FR68:** 配置 CORS 支援前端開發
- **FR69:** 提供 Postman Collection
- **FR70:** 提供 SAGA 補償邏輯 Integration Tests
- **FR71:** 提供 Helm 部署 Smoke Tests

**Phase 2 延後項目（FR59-65）：**
- Workshop 教學材料（7 項功能需求）

---

## Epic Structure Plan

基於 PRD 的 64 條功能需求和 Architecture 的技術決策，本專案規劃為 **6 個 Epic**，遵循使用者價值遞增原則：

### Epic 1: Foundation & Core Infrastructure Setup
**使用者價值：** 建立完整的技術基礎，使所有後續功能可以運作
**FR 覆蓋：** FR47-FR53 (Deployment), FR54-FR58 (Auth & Security), FR66-FR68 (API Contract), Architecture Decision 3.2 (API Documentation)
**技術脈絡：** K8s namespaces, PostgreSQL, Kafka, Grafana Stack, OTel Collector, JWT authentication, Swagger UI/OpenAPI
**Story 數量：** ~13 stories

### Epic 2: Basic Banking Operations with Complete Observability
**使用者價值：** 使用者可以進行基本銀行操作（存款、提款、查詢），並且所有操作都有完整的 trace 可視化
**FR 覆蓋：** FR1-FR3 (Account & Basic Ops), FR9-FR15 (Transaction History), FR16-FR26 (OTel Instrumentation), FR27-FR33 (Grafana Integration)
**技術脈絡：** Account Service, Transaction Service, Deposit/Withdrawal Service, 完整 OpenTelemetry 手動 SDK
**Story 數量：** ~15 stories

### Epic 3: Advanced Transfer Operations with SAGA Pattern
**使用者價值：** 使用者可以進行跨帳戶轉帳和跨幣別換匯，系統保證分散式交易一致性
**FR 覆蓋：** FR4-FR8 (Transfer & Exchange), FR34-FR40 (Observability Demo Capabilities)
**技術脈絡：** Transfer Service (協調層 SAGA Orchestrator), Exchange Service (協調層), 呼叫 Transaction Service 執行金流
**Story 數量：** ~10 stories

### Epic 4: Chaos Engineering Integration & Root Cause Analysis
**使用者價值：** 演講者可以在 Workshop 中展示故障注入與快速定位，證明可觀測性的價值
**FR 覆蓋：** FR41-FR46 (Chaos Engineering)
**技術脈絡：** Chaos Mesh (NetworkChaos, PodChaos, StressChaos), Grafana Tempo failure visualization
**Story 數量：** ~6 stories

### Epic 5: API Testing & Quality Assurance
**使用者價值：** 開發者和 QA 可以完整測試所有 API 功能，確保系統穩定性
**FR 覆蓋：** FR69-FR71 (Testing)
**技術脈絡：** Postman Collection, Integration Tests, Smoke Tests
**Story 數量：** ~3 stories

### Epic 6: Documentation & Deployment Automation
**使用者價值：** 觀眾可以獨立完成本地部署，學習和驗證專案
**FR 覆蓋：** FR52-FR53 (Documentation - 完整實作)
**技術脈絡：** README.md, DEPLOYMENT.md, troubleshooting guides
**Story 數量：** ~2 stories

**總計：** 6 Epics, ~48 Stories, 覆蓋 64 FRs

---

## Epic 1: Foundation & Core Infrastructure Setup

**Epic Goal:** 建立完整的技術基礎設施，包含資料庫、訊息佇列、可觀測性平台、認證系統、API 文件化工具，使所有後續使用者功能可以運作。

**PRD Coverage:** FR47-FR53, FR54-FR58, FR66-FR68, Architecture Decision 3.2

**Technical Context:**
- Kubernetes 4 namespaces (services, infra, observability, chaos-mesh)
- PostgreSQL with Flyway migrations
- Kafka KRaft mode
- Grafana Stack (Tempo, Loki, Prometheus, Grafana)
- OpenTelemetry Collector
- API Gateway with JWT validation
- Nginx Ingress with security headers
- Swagger UI / OpenAPI 3.0 Documentation for all microservices

---

### Story 1.1: Kubernetes Namespace & Resource Quota Setup

As a **DevOps Engineer**,
I want to create isolated Kubernetes namespaces with resource quotas,
So that different system components are properly isolated and resource usage is controlled.

**Acceptance Criteria:**

Given I have a running Kubernetes cluster (v1.32-v1.34)
When I apply namespace manifests
Then 4 namespaces are created: `lite-bank-services`, `lite-bank-infra`, `lite-bank-observability`, `chaos-mesh`

And each namespace has resource quota configured:
- lite-bank-services: requests.cpu=4, requests.memory=8Gi, limits.cpu=8, limits.memory=16Gi
- lite-bank-infra: requests.cpu=2, requests.memory=4Gi, limits.cpu=4, limits.memory=8Gi
- lite-bank-observability: requests.cpu=2, requests.memory=4Gi, limits.cpu=4, limits.memory=8Gi

And I can verify with `kubectl get namespaces` and `kubectl describe resourcequota -n {namespace}`

**Technical Implementation:**
- Create 4 YAML files: `namespaces/namespace-services.yaml`, `namespace-infra.yaml`, `namespace-observability.yaml`, `namespace-chaos.yaml`
- Each includes namespace definition + ResourceQuota object
- Labels: `project=lite-bank-demo`, `managed-by=helm`

**Prerequisites:** None (first story)

---

### Story 1.2: PostgreSQL Database Deployment with Persistent Storage

As a **DevOps Engineer**,
I want to deploy PostgreSQL with persistent storage in Kubernetes,
So that all microservices can store and retrieve data reliably.

**Acceptance Criteria:**

Given namespace `lite-bank-infra` exists
When I deploy PostgreSQL using StatefulSet
Then PostgreSQL Pod is running with status=Running

And PersistentVolumeClaim is bound with 10Gi storage
And database is accessible at `postgresql.lite-bank-infra.svc.cluster.local:5432`
And I can connect using psql client with credentials from Secret

And database has initialized with empty database `litebank`

**Technical Implementation:**
- StatefulSet with 1 replica (demo environment)
- Image: `postgres:16-alpine`
- PVC: 10Gi, storageClass=standard
- Secret: `postgresql-credentials` with POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB
- Service: ClusterIP exposing port 5432
- ConfigMap: `postgresql-config` with `max_connections=100`, `shared_buffers=256MB`
- Liveness probe: `pg_isready -U postgres`
- Readiness probe: `pg_isready -U postgres`

**Prerequisites:** Story 1.1

---

### Story 1.3: Database Schema Initialization with Flyway

As a **Backend Developer**,
I want to initialize database schemas using Flyway migrations,
So that all required tables are created with proper structure.

**Acceptance Criteria:**

Given PostgreSQL is running
When I execute Flyway migration scripts
Then the following tables are created:
- `users` (user_id, username, email, password_hash, created_at, updated_at)
- `accounts` (account_id, user_id, currency, balance, status, created_at, updated_at)
- `transactions` (transaction_id, account_id, transaction_type, amount, currency, balance_after, reference_id, description, trace_id, created_at)
- `saga_executions` (saga_id, transaction_id, current_step, status, compensate_from_step, error_message, created_at, updated_at)

And all indexes are created:
- idx_accounts_user_id, idx_accounts_currency
- idx_transactions_account_id, idx_transactions_created_at, idx_transactions_trace_id
- idx_saga_transaction_id, idx_saga_status

And all foreign key constraints are applied
And `transactions` table has CHECK constraint: `balance_after >= 0`

**Technical Implementation:**
- Flyway version: 10.x
- Migration files: `db/migration/V1__create_users_table.sql`, `V2__create_accounts_table.sql`, `V3__create_transactions_table.sql`, `V4__create_saga_executions_table.sql`
- Naming convention: snake_case for tables/columns
- Execute via Flyway Docker image as Kubernetes Job
- Connection string: `jdbc:postgresql://postgresql.lite-bank-infra:5432/litebank`

**Prerequisites:** Story 1.2

---

### Story 1.4: Kafka KRaft Deployment for Asynchronous Communication

As a **Backend Developer**,
I want to deploy Kafka in KRaft mode (no ZooKeeper),
So that microservices can communicate asynchronously via message queues.

**Acceptance Criteria:**

Given namespace `lite-bank-infra` exists
When I deploy Kafka using StatefulSet
Then Kafka broker is running with status=Running

And Kafka is accessible at `kafka.lite-bank-infra.svc.cluster.local:9092`
And topic `banking.notifications` is created with 3 partitions, replication-factor=1
And I can produce and consume messages using kafka-console tools

And Kafka metrics are exposed on port 9101 for Prometheus scraping

**Technical Implementation:**
- StatefulSet with 1 replica (demo environment)
- Image: `confluentinc/cp-kafka:7.5.0`
- KRaft mode configuration (no ZooKeeper)
- Environment variables: KAFKA_PROCESS_ROLES=broker,controller, KAFKA_NODE_ID=1, KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka-0:9093
- PVC: 5Gi for data persistence
- Service: ClusterIP exposing port 9092 (client), 9093 (controller)
- Create topic via kafka-topics.sh in init container

**Prerequisites:** Story 1.1

---

### Story 1.5: OpenTelemetry Collector Deployment

As a **SRE**,
I want to deploy OpenTelemetry Collector to receive and export traces/logs/metrics,
So that all microservices can send observability data to Grafana Stack.

**Acceptance Criteria:**

Given namespace `lite-bank-observability` exists
When I deploy OTel Collector using Deployment
Then OTel Collector Pod is running with status=Running

And OTel Collector receives traces on OTLP HTTP (4318) and OTLP gRPC (4317)
And traces are exported to Grafana Tempo at `tempo.lite-bank-observability:4317`
And logs are exported to Grafana Loki at `loki.lite-bank-observability:3100`
And metrics are exported to Prometheus at `prometheus.lite-bank-observability:9090`

And sampling strategy is configured to 100% (Phase 1 demo)

**Technical Implementation:**
- Deployment with 1 replica
- Image: `otel/opentelemetry-collector-contrib:0.91.0`
- ConfigMap: `otel-collector-config` with receivers (otlp), processors (batch, probabilistic_sampler), exporters (otlp/tempo, loki, prometheus)
- Sampling: `sampling_percentage: 100` in config
- Resource limits: cpu=1000m, memory=2Gi
- Service: ClusterIP exposing 4317 (gRPC), 4318 (HTTP), 8888 (metrics)

**Prerequisites:** Story 1.1

---

### Story 1.6: Grafana Tempo Deployment for Distributed Tracing

As a **SRE**,
I want to deploy Grafana Tempo to store and query distributed traces,
So that演講者 can visualize complete request traces in Workshop demos.

**Acceptance Criteria:**

Given namespace `lite-bank-observability` exists
When I deploy Grafana Tempo using StatefulSet
Then Tempo Pod is running with status=Running

And Tempo receives traces from OTel Collector on port 4317
And Tempo exposes query API on port 3200
And Tempo data retention is configured to 7 days
And Tempo compactor runs successfully

And I can query traces via `curl http://tempo:3200/api/search`

**Technical Implementation:**
- StatefulSet with 1 replica
- Image: `grafana/tempo:2.3.0`
- PVC: 20Gi for trace storage
- ConfigMap: `tempo-config` with compactor.retention_period=7d
- Service: ClusterIP exposing 4317 (OTLP gRPC), 3200 (Query)
- Storage backend: local filesystem (demo), production should use S3/GCS

**Prerequisites:** Story 1.5

---

### Story 1.7: Grafana Loki Deployment for Log Aggregation

As a **SRE**,
I want to deploy Grafana Loki to aggregate and query logs,
So that all microservices logs are centralized and searchable.

**Acceptance Criteria:**

Given namespace `lite-bank-observability` exists
When I deploy Grafana Loki using StatefulSet
Then Loki Pod is running with status=Running

And Loki receives logs from OTel Collector on port 3100
And Loki data retention is configured to 14 days
And Loki compactor runs successfully

And I can query logs via LogQL: `{service_name="account-service"}`

**Technical Implementation:**
- StatefulSet with 1 replica
- Image: `grafana/loki:2.9.0`
- PVC: 10Gi for log storage
- ConfigMap: `loki-config` with table_manager.retention_period=14d, compactor enabled
- Service: ClusterIP exposing 3100 (Push/Query)

**Prerequisites:** Story 1.5

---

### Story 1.8: Prometheus Deployment for Metrics Collection

As a **SRE**,
I want to deploy Prometheus to scrape and store metrics,
So that microservices performance metrics are available for monitoring.

**Acceptance Criteria:**

Given namespace `lite-bank-observability` exists
When I deploy Prometheus using StatefulSet
Then Prometheus Pod is running with status=Running

And Prometheus scrapes metrics from all microservices every 15 seconds
And Prometheus scrapes metrics from Kafka on port 9101
And Prometheus scrapes metrics from OTel Collector on port 8888

And I can query metrics via PromQL: `http_server_requests_total`
And scrape targets show status=UP for all services

**Technical Implementation:**
- StatefulSet with 1 replica
- Image: `prom/prometheus:v2.48.0`
- PVC: 10Gi for metrics storage
- ConfigMap: `prometheus-config` with scrape_configs for all services
- Scrape interval: 15s
- Service: ClusterIP exposing 9090 (Query/UI)
- ServiceMonitor CRDs for auto-discovery (if Prometheus Operator used)

**Prerequisites:** Story 1.5

---

### Story 1.9: Grafana Deployment with Data Source Configuration

As a **Workshop Speaker**,
I want to deploy Grafana with pre-configured data sources,
So that I can immediately visualize traces, logs, and metrics without manual setup.

**Acceptance Criteria:**

Given Tempo, Loki, Prometheus are running
When I deploy Grafana using Deployment
Then Grafana Pod is running with status=Running

And Grafana is accessible at `http://grafana.lite-bank-observability:3000`
And I can login with default credentials (admin/admin)

And data sources are pre-configured:
- Tempo: url=http://tempo:3200, configured with derived fields for Loki correlation
- Loki: url=http://loki:3100, configured with derived fields for Tempo correlation
- Prometheus: url=http://prometheus:9090, configured with exemplars enabled

And I can execute test queries on all data sources successfully

**Technical Implementation:**
- Deployment with 1 replica
- Image: `grafana/grafana:10.2.0`
- ConfigMap: `grafana-datasources` with provisioning YAML for Tempo, Loki, Prometheus
- Derived fields configuration:
  - Tempo → Loki: trace_id field → Loki query `{trace_id="${__value.raw}"}`
  - Loki → Tempo: trace_id field → Tempo query by trace ID
  - Prometheus → Tempo: exemplar trace_id → Tempo query
- Service: ClusterIP exposing 3000
- Persistent volume: 5Gi for dashboards

**Prerequisites:** Story 1.6, 1.7, 1.8

---

### Story 1.10: User Service with JWT Authentication

As a **Backend Developer**,
I want to implement User Service with JWT authentication,
So that users can login and obtain tokens for API access.

**Acceptance Criteria:**

Given database schema is initialized
When I deploy User Service using Deployment
Then User Service Pod is running with status=Running

And `POST /api/v1/auth/login` endpoint is available:
- Request: `{"username": "alice", "password": "password123"}`
- Response 200: `{"token": "eyJ...", "expires_in": 86400}`
- Response 401 on invalid credentials: `{"error": {"code": "ERR_AUTH_001", "message": "Invalid credentials"}}`

And JWT token contains claims: `user_id`, `username`, `iat`, `exp`
And JWT token is signed with HS256 algorithm using secret from Kubernetes Secret
And passwords are hashed using bcrypt with cost=10

And User Service exposes `/actuator/health/liveness` and `/actuator/health/readiness` endpoints

**Technical Implementation:**
- Java SpringBoot 3.2.x
- Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-security-crypto, jjwt (JWT library)
- Database: Connect to PostgreSQL, access `users` table
- Password hashing: BCryptPasswordEncoder
- JWT secret: Read from env var `JWT_SECRET` (from Kubernetes Secret)
- Token expiration: 24 hours (demo)
- Dockerfile: Multi-stage build (Maven + JRE17-alpine)
- Kubernetes: Deployment with 1 replica, Service ClusterIP port 8080

**Prerequisites:** Story 1.3

---

### Story 1.11: API Gateway with Centralized JWT Validation

As a **Backend Developer**,
I want to deploy API Gateway that validates JWT tokens,
So that all backend services can trust authenticated requests without individual validation.

**Acceptance Criteria:**

Given User Service is running
When I deploy API Gateway using Deployment
Then API Gateway Pod is running with status=Running

And API Gateway validates JWT token from `Authorization: Bearer {token}` header:
- Valid token: Forward request to backend with `X-User-Id` header injected
- Invalid token: Return 401 `{"error": {"code": "ERR_AUTH_002", "message": "Invalid token"}}`
- Expired token: Return 401 `{"error": {"code": "ERR_AUTH_003", "message": "Token expired"}}`
- Missing token: Return 401 `{"error": {"code": "ERR_AUTH_004", "message": "Missing authorization header"}}`

And API Gateway routes requests to backend services:
- `/api/v1/auth/*` → User Service (no JWT validation)
- `/api/v1/accounts/*` → Account Service (JWT required) - 資料層，只讀
- `/api/v1/transactions/*` → Transaction Service (JWT required) - 資料層，唯一可修改餘額
- `/api/v1/deposits/*`, `/api/v1/withdrawals/*` → Deposit/Withdrawal Service (JWT required) - 協調層
- `/api/v1/transfers/*` → Transfer Service (JWT required) - 協調層
- `/api/v1/exchanges/*` → Exchange Service (JWT required) - 協調層
- `/api/v1/rates/*` → Exchange Rate Service (no JWT validation) - 資料層

And API Gateway adds trace context headers: `traceparent`, `X-Request-ID`
And CORS is configured: allow origin `http://localhost:3000`, allow credentials

**Technical Implementation:**
- Spring Cloud Gateway 3.1.x (Java)
- JWT validation: Custom GatewayFilter reading JWT_SECRET from env
- Route configuration in application.yml
- CORS configuration: globalcors section
- Trace context: Spring Cloud Sleuth auto-instrumentation
- Service: ClusterIP port 8080, exposed via Ingress

**Prerequisites:** Story 1.10

---

### Story 1.12: Unified API Response & Error Format Implementation

As a **Backend Developer**,
I want all microservices to use unified response and error formats,
So that frontend and API consumers have consistent contract.

**Acceptance Criteria:**

Given all microservices are deployed
When any microservice returns a success response
Then response format is:
```json
{
  "success": true,
  "data": {...},
  "trace_id": "abc123def456",
  "timestamp": "2025-12-15T10:00:00.123Z"
}
```

When any microservice returns an error response
Then response format is:
```json
{
  "success": false,
  "error": {
    "code": "ERR_001",
    "type": "INSUFFICIENT_BALANCE",
    "message": "餘額不足",
    "category": "business",
    "details": {...},
    "trace_id": "abc123def456",
    "timestamp": "2025-12-15T10:00:00.123Z"
  }
}
```

And all Java services use `@RestControllerAdvice` for global exception handling
And all Python services use FastAPI exception handlers
And trace_id is extracted from OpenTelemetry Span context

**Technical Implementation:**
- Java: Create `GlobalExceptionHandler` with `@RestControllerAdvice`
- Java: Create `ApiResponse<T>` wrapper class
- Python: Create FastAPI `@app.exception_handler` decorators
- Python: Create Pydantic models for ErrorResponse
- Error codes mapping: ERR_001 to ERR_099 (defined in Architecture doc)
- Extract trace_id: `Span.current().getSpanContext().getTraceId()` (Java), `trace.get_current_span().get_span_context().trace_id` (Python)

**Prerequisites:** Story 1.11

---

### Story 1.13: Add Swagger UI/OpenAPI Documentation to All Microservices

**Story ID:** STORY-1.13
**Epic:** Epic 1 - Foundation & Core Infrastructure Setup
**User Value:** 開發者和測試人員可以透過互動式 API 文件快速理解和測試所有微服務端點

**Description:**
為所有 7 個 SpringBoot 微服務加入 Swagger UI / OpenAPI 3.0 文件支援，提供互動式 API 文件介面，符合 Architecture.md Section 3.2 的技術決策。

**Scope:**
實作範圍包含以下微服務：
- user-service (port 8080)
- account-service (port 8081)
- transaction-service (port 8082)
- transfer-service (port 8083)
- exchange-rate-service (port 8084)
- exchange-service (port 8085)
- deposit-withdrawal-service (port 8086)

**Technical Requirements:**

1. **Maven 依賴配置:**
   - 在所有服務的 `pom.xml` 加入 `springdoc-openapi-starter-webmvc-ui` (version 2.3.0)
   - 確保與 Spring Boot 3.4.1 相容

2. **API Documentation Endpoints:**
   - Swagger UI: `http://localhost:{port}/swagger-ui.html`
   - OpenAPI JSON: `http://localhost:{port}/v3/api-docs`
   - 確保所有端點可正常訪問

3. **Controller Annotations:**
   - 在主要 Controller 加入 `@Tag` annotation 描述 API 群組
   - 關鍵端點加入 `@Operation` 描述功能
   - Request/Response DTO 加入 `@Schema` 描述欄位

4. **Documentation Content:**
   - 所有 endpoints 包含:
     - Request/Response schemas
     - Example payloads
     - Error responses (包含錯誤碼對照)
     - Trace context headers 說明 (`X-Trace-Id`)

5. **Application Configuration:**
   - 在 `application.yml` 設定 Swagger UI 路徑
   - 設定 API info (title, version, description)
   - 設定 server URL

**Acceptance Criteria:**

**AC1: Maven 依賴正確配置**
- GIVEN 所有 7 個微服務的 pom.xml
- WHEN 執行 `mvn dependency:tree`
- THEN 應該看到 `springdoc-openapi-starter-webmvc-ui:2.3.0`

**AC2: Swagger UI 可訪問**
- GIVEN 所有微服務已啟動
- WHEN 訪問 `http://localhost:{port}/swagger-ui.html`
- THEN 應該看到 Swagger UI 介面並列出所有 API 端點

**AC3: OpenAPI JSON 規格可下載**
- GIVEN 所有微服務已啟動
- WHEN 訪問 `http://localhost:{port}/v3/api-docs`
- THEN 應該回傳有效的 OpenAPI 3.0 JSON 規格

**AC4: API 文件包含必要資訊**
- GIVEN Swagger UI 已開啟
- WHEN 檢視任一 API 端點
- THEN 應該包含:
  - 端點描述
  - Request body schema (若適用)
  - Response schema
  - 至少一個 error response 範例

**AC5: Trace Context 文件化**
- GIVEN Swagger UI 已開啟
- WHEN 檢視任一需要認證的端點
- THEN 應該在 headers 說明中看到 `X-Trace-Id` 的描述

**Implementation Notes:**
- 參考 Architecture.md Section 3.2 的完整規範
- 優先順序: user-service 和 account-service 先實作，其他服務跟進
- 可選: 考慮在 API Gateway 加入聚合所有服務 API 文件的功能 (Phase 2)

**Testing Strategy:**
1. 手動測試: 開啟每個服務的 Swagger UI，執行 "Try it out" 功能
2. 自動化測試: 加入測試驗證 `/v3/api-docs` 端點回傳有效 JSON
3. 整合測試: 確保加入 Swagger 後原有功能不受影響

**Prerequisites:** Story 1.10, Story 1.11, Story 1.12

**Estimated Effort:** 4-6 hours
- 每個服務約 30-40 分鐘
- 包含測試和文件驗證時間

**FR Coverage:** Architecture Decision 3.2 (API Documentation)

---

## Epic 2: Basic Banking Operations with Complete Observability

**Epic Goal:** 使用者可以進行基本銀行操作（查詢帳戶、存款、提款），所有操作都有完整的 OpenTelemetry trace，並可在 Grafana Tempo 中視覺化。

**PRD Coverage:** FR1-FR3, FR9-FR15, FR16-FR26, FR27-FR33

**Stories (Summary):**
- **Story 2.1:** Account Service - Query All Accounts (FR1)
- **Story 2.2:** Account Service - Deposit Operation (FR2)
- **Story 2.3:** Account Service - Withdrawal Operation (FR3)
- **Story 2.4:** Transaction Service - Query Transaction History (FR9, FR10)
- **Story 2.5:** Transaction Service - Record Complete Transaction Info (FR11-FR15)
- **Story 2.6-2.16:** Complete OpenTelemetry Manual SDK Instrumentation across all services (FR16-FR26):
  - Configure Sampling Strategy
  - Generate & Propagate trace_id
  - Create Spans with Business Attributes
  - Structured Logging with trace_id/span_id
  - W3C TraceContext in HTTP headers
  - API responses include X-Trace-Id
- **Story 2.17-2.19:** Grafana Stack Integration (FR27-FR33):
  - Export to Tempo/Loki/Prometheus
  - Configure Data Source Correlations
  - Set Retention Periods

---

## Epic 3: Advanced Transfer Operations with SAGA Pattern

**Epic Goal:** 使用者可以進行跨帳戶轉帳和跨幣別換匯，系統保證分散式交易一致性，SAGA 流程完整可視化。

**PRD Coverage:** FR4-FR8, FR34-FR40

**Stories (Summary):**
- **Story 3.1:** Exchange Rate Service - Mock Exchange Rate API (FR6) - 資料層
- **Story 3.2:** Exchange Service - Exchange Operation (FR5) - 協調層，呼叫 Account Service 驗證 + Transaction Service 執行金流
- **Story 3.3:** Transfer Service - Same-Currency Transfer (FR4) - 協調層，呼叫 Account Service 驗證 + Transaction Service 執行金流
- **Story 3.4:** Transfer Service - SAGA Orchestration Flow (FR4, FR5):
  - validate-accounts → debit-from-source → credit-to-destination → record-transactions
- **Story 3.5:** SAGA Compensation Logic Implementation (FR7) - 由協調層處理，反向呼叫 Transaction Service
- **Story 3.6:** Transaction Idempotency Implementation (FR8)
- **Story 3.7-3.10:** Observability Demo Capabilities (FR34-FR40):
  - Query traces by trace_id
  - log ↔ trace ↔ metric three-way jumping
  - SAGA visualization in Tempo
  - Quick failure location (1-2 minutes)

---

## Epic 4: Chaos Engineering Integration & Root Cause Analysis

**Epic Goal:** 演講者可以在 Workshop 中展示故障注入與快速定位，證明可觀測性的決定性價值。

**PRD Coverage:** FR41-FR46

**Stories (Summary):**
- **Story 4.1:** Chaos Mesh Deployment in chaos-mesh Namespace
- **Story 4.2:** NetworkChaos - Inject 5-Second Delay to Exchange Service (FR41)
- **Story 4.3:** PodChaos - Inject Pod Termination (FR42)
- **Story 4.4:** StressChaos - Inject CPU/Memory Pressure (FR43)
- **Story 4.5:** Chaos Failure Visualization in Grafana Tempo (FR44):
  - Red spans for ERROR status
  - Duration > 5000ms clearly visible
  - Error messages in span details
- **Story 4.6:** Chaos Recovery & Root Cause Location Demo (FR45, FR46):
  - Clear NetworkChaos resource
  - System recovers within 30 seconds
  - Demo: From failure injection to root cause in Tempo < 2 minutes

---

## Epic 5: API Testing & Quality Assurance

**Epic Goal:** 開發者和 QA 可以完整測試所有 API 功能，確保系統穩定性。

**PRD Coverage:** FR69-FR71

**Stories (Summary):**
- **Story 5.1:** Postman Collection for All API Endpoints (FR69):
  - Login, Account Query, Deposit, Withdrawal, Transfer, Exchange, Transaction History
  - Error scenarios (insufficient balance, invalid amount, unauthorized)
  - Environment variables (base_url, jwt_token)
- **Story 5.2:** Integration Tests for SAGA Compensation (FR70):
  - Simulate Exchange Service failure 100 times
  - Verify compensation logic execution
  - Assert balance restored + status=COMPENSATED
  - Coverage > 80% for SAGA code
- **Story 5.3:** Smoke Tests for Helm Deployment (FR71):
  - Check all Pods status=Running
  - Test /health/liveness endpoints
  - Execute one complete transaction flow
  - Verify basic functions operational

---

## Epic 6: Documentation & Deployment Automation

**Epic Goal:** 觀眾可以獨立完成本地部署，學習和驗證專案。

**PRD Coverage:** FR52-FR53 (完整實作)

**Stories (Summary):**
- **Story 6.1:** Complete README.md with Quick Start Guide (FR52):
  - Prerequisites (K8s 1.32-1.34, Helm 3.x, kubectl)
  - Quick Start (helm install command)
  - System architecture diagram
  - Basic usage examples
  - Target: 5-minute system startup
- **Story 6.2:** Complete DEPLOYMENT.md with Detailed Instructions (FR53):
  - Helm Chart parameter descriptions
  - Environment variable list
  - values.yaml customization guide
  - Troubleshooting guide (Pod startup failure, Grafana connection issues)
  - Target: Independent deployment within 15 minutes

---

## FR Coverage Matrix

**Complete mapping of all 64 Functional Requirements to Epic Stories:**

### Epic 1: Foundation & Core Infrastructure (12 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR47 | 透過 Helm Charts 一鍵部署 | 1.1-1.12 | ✅ K8s setup, all services deployable |
| FR48 | 透過 Docker Compose 快速啟動 | 1.1-1.12 | ✅ Docker images + compose.yml |
| FR49 | 配置 Liveness Probes | 1.10-1.12 | ✅ All services |
| FR50 | 配置 Readiness Probes | 1.10-1.12 | ✅ All services |
| FR51 | Helm values.yaml 環境配置 | 1.1-1.12 | ✅ values-dev/staging/prod.yaml |
| FR54 | JWT 登入 | 1.10 | ✅ POST /api/v1/auth/login |
| FR55 | API Gateway JWT 驗證 | 1.11 | ✅ Centralized validation |
| FR56 | JWT 包含 user_id/account_ids | 1.10 | ✅ Token claims |
| FR57 | bcrypt 雜湊密碼 | 1.10 | ✅ BCryptPasswordEncoder |
| FR58 | HTTPS/TLS via Ingress | 1.11 | ✅ Nginx Ingress + cert |
| FR66 | 統一 API Response 格式 | 1.12 | ✅ Success response wrapper |
| FR67 | 統一 API Error 格式 | 1.12 | ✅ Error response wrapper |
| FR68 | CORS 配置 | 1.11 | ✅ Gateway CORS config |

### Epic 2: Basic Banking Operations (19 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR1 | 查詢所有帳戶 | 2.1 | ✅ GET /api/v1/accounts |
| FR2 | 存款至帳戶 | 2.2 | ✅ POST /api/v1/deposits |
| FR3 | 從帳戶提款 | 2.3 | ✅ POST /api/v1/withdrawals |
| FR9 | 查詢交易歷史 | 2.4 | ✅ GET /api/v1/transactions?accountId=... |
| FR10 | 日期範圍過濾 | 2.4 | ✅ Query params startDate/endDate |
| FR11 | 記錄完整交易資訊 | 2.5 | ✅ All fields in transactions table |
| FR12 | Append-Only Transaction Table | 2.5 | ✅ No UPDATE/DELETE operations |
| FR13 | 記錄交易狀態變更 | 2.5 | ✅ ENUM status field |
| FR14 | SAGA metadata 補償記錄 | 2.5 | ✅ JSON metadata field |
| FR15 | 永久儲存 trace_id | 2.5 | ✅ trace_id VARCHAR(32) indexed |
| FR16 | Sampling 策略 100% | 2.6 | ✅ OTel Collector config |
| FR17 | 生成 trace_id 並傳播 | 2.7 | ✅ All services |
| FR18 | Span duration & status | 2.8 | ✅ All operations |
| FR19 | Span business attributes | 2.9 | ✅ account.id, amount, currency |
| FR20 | Span 命名與層級結構 | 2.10 | ✅ Parent-child hierarchy |
| FR21 | 補償 span | 2.11 | ✅ unfreeze-balance, rollback |
| FR22 | Log 包含 trace_id/span_id | 2.12 | ✅ Structured JSON logs |
| FR23 | Kafka W3C TraceContext | 2.13 | ✅ traceparent header |
| FR24 | 服務間 trace propagation | 2.14 | ✅ 協調層→Account Service→Transaction Service |
| FR25 | Response Header X-Trace-Id | 2.15 | ✅ All API responses |
| FR26 | Error body 包含 trace_id | 2.16 | ✅ ErrorResponse model |
| FR27 | 匯出至 Tempo | 2.17 | ✅ OTLP exporter |
| FR28 | 匯出至 Loki | 2.17 | ✅ Loki appender |
| FR29 | 匯出至 Prometheus | 2.17 | ✅ /actuator/prometheus |
| FR30 | Tempo ↔ Loki 關聯 | 2.18 | ✅ Derived fields config |
| FR31 | Prometheus exemplar → Tempo | 2.18 | ✅ Exemplar config |
| FR32 | Tempo 保留 7 天 | 2.19 | ✅ retention_period=7d |
| FR33 | Loki 保留 14 天 | 2.19 | ✅ retention_period=14d |

### Epic 3: SAGA Transfer Operations (10 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR4 | 同幣別轉帳 | 3.3 | ✅ POST /api/v1/transfers |
| FR5 | 跨幣別換匯轉帳 | 3.2, 3.4 | ✅ POST /api/v1/exchanges |
| FR6 | 查詢即時匯率 | 3.1 | ✅ GET /api/v1/rates/{from}/{to} |
| FR7 | SAGA 補償邏輯 | 3.5 | ✅ Automatic compensation |
| FR8 | 交易冪等性 | 3.6 | ✅ transaction_id uniqueness |
| FR34 | 查詢 trace by trace_id | 3.7 | ✅ Grafana Tempo query |
| FR35 | Tempo → Loki 跳轉 | 3.8 | ✅ "View Logs" button |
| FR36 | Loki → Tempo 跳轉 | 3.8 | ✅ Clickable trace_id link |
| FR37 | Prometheus → Tempo 跳轉 | 3.8 | ✅ Exemplar click |
| FR38 | Tempo 視覺化 SAGA 6 步驟 | 3.9 | ✅ Clear span hierarchy |
| FR39 | Tempo 視覺化補償邏輯 | 3.9 | ✅ Compensation spans |
| FR40 | 快速定位失敗點 | 3.10 | ✅ ERROR spans in red |

### Epic 4: Chaos Engineering (6 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR41 | NetworkChaos 網路延遲 | 4.2 | ✅ 5-second delay injection |
| FR42 | PodChaos Pod 終止 | 4.3 | ✅ Pod termination |
| FR43 | StressChaos CPU/Memory 壓力 | 4.4 | ✅ Resource stress |
| FR44 | Chaos 影響反映於 Tempo | 4.5 | ✅ Red spans, duration spike |
| FR45 | Chaos 可控且可恢復 | 4.6 | ✅ 30-second recovery |
| FR46 | 1-2 分鐘定位根因 | 4.6 | ✅ Tempo quick diagnosis |

### Epic 5: Testing & QA (3 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR69 | Postman Collection | 5.1 | ✅ All endpoints + error scenarios |
| FR70 | SAGA 補償 Integration Tests | 5.2 | ✅ 100 failure simulations |
| FR71 | Helm 部署 Smoke Tests | 5.3 | ✅ Complete flow validation |

### Epic 6: Documentation (2 Stories)

| FR | Requirement | Epic.Story | Implementation Status |
|----|-------------|------------|----------------------|
| FR52 | 基礎 README.md | 6.1 | ✅ 5-minute quick start |
| FR53 | 基礎 DEPLOYMENT.md | 6.2 | ✅ 15-minute deployment guide |

---

## Coverage Summary

**Total Requirements Covered:** 64/64 (100%)

**Epic Distribution:**
- Epic 1 (Foundation): 13 FRs
- Epic 2 (Basic Banking + Observability): 28 FRs
- Epic 3 (SAGA Transfers): 12 FRs
- Epic 4 (Chaos Engineering): 6 FRs
- Epic 5 (Testing): 3 FRs
- Epic 6 (Documentation): 2 FRs

**Story Count:** ~52 stories (12+19+10+6+3+2)

**Phase 1 MVP Status:** All 64 FRs are covered and implementation-ready with complete technical context from Architecture.md

---

## Next Steps

✅ **Epic & Story Breakdown Complete!**

**Ready for Phase 4: Sprint Planning & Implementation**

這份 Epic Breakdown 文件已整合 PRD 的所有功能需求和 Architecture 的完整技術脈絡，每個 Story 都包含：
- 明確的使用者價值
- 完整的驗收標準
- 具體的技術實作細節（API 端點、資料庫 Schema、OTel 模式、錯誤處理等）
- 清楚的前置依賴關係

開發團隊可以直接根據這些 Stories 開始實作，無需額外的技術澄清。

**建議下一個工作流程：**
```
/bmad:bmm:workflows:sprint-planning
```

此工作流程將建立 Sprint 狀態追蹤文件，組織這 52 個 Stories 為可執行的 Sprint 計畫。

---

_文件生成日期: 2025-12-15_
_PRD 版本: 2025-12-15 (64 FRs)_
_Architecture 版本: 2025-12-09 (完整技術脈絡)_
