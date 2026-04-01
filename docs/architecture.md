# LiteBank Architecture

## System Overview

LiteBank is a microservices banking system with 10 services (9 custom + API Gateway), demonstrating coordination layer architecture, pessimistic-lock-based distributed transactions, and full OpenTelemetry observability.

## Architecture Diagram

```mermaid
graph TB
    %% ===== External =====
    User([User / Browser])

    %% ===== Gateway Layer =====
    subgraph Gateway["Gateway Layer"]
        GW[API Gateway<br/>:8080<br/>JWT Validation]
        Dashboard[Dashboard<br/>:80<br/>React + Nginx]
    end

    %% ===== Coordination Layer (Active) =====
    subgraph Coordination["Coordination Layer — Orchestrate workflows, never write DB directly"]
        Teller[Teller Service<br/>:8083<br/>Transfer + Deposit + Withdrawal]
        Exchange[Exchange Service<br/>:8085]
    end

    %% ===== Data Layer (Passive) =====
    subgraph DataLayer["Data Layer — Query, validate, and store"]
        UserSvc[User Service<br/>:8080<br/>Auth + JWT]
        Account[Account Service<br/>:8081<br/>READ-ONLY]
        Transaction[Transaction Service<br/>:8082<br/>ONLY writes balances]
        ExchangeRate[Exchange Rate Service<br/>:8084<br/>Stateless]
    end

    %% ===== Event-Driven Layer =====
    subgraph EventDriven["Event-Driven Layer — Async consumers"]
        AnalyticsProcessor[Analytics Processor<br/>:8087<br/>Kafka Consumer]
        AnalyticsQuery[Analytics Query<br/>:8088<br/>REST API]
        Notification[Notification Service<br/>:8089<br/>SSE Push]
    end

    %% ===== Infrastructure =====
    subgraph Infra["Infrastructure"]
        PG[(PostgreSQL :5432)]
        Kafka[Kafka :9092<br/>KRaft Mode]
    end

    %% ===== Observability =====
    subgraph Observability["Observability Stack"]
        OTel[OTel Collector<br/>:4317/:4318]
        Tempo[Tempo<br/>:3200<br/>Traces]
        Loki[Loki<br/>:3100<br/>Logs]
        Prometheus[Prometheus<br/>:9090<br/>Metrics]
        Grafana[Grafana<br/>:3000]
    end

    %% ===== User Flows =====
    User -->|HTTPS| GW
    User -->|HTTPS| Dashboard
    Dashboard -->|/api/*| GW

    %% ===== Gateway → Services =====
    GW -->|X-User-ID header| UserSvc
    GW --> Teller
    GW --> Exchange
    GW --> Account
    GW --> AnalyticsQuery
    GW -->|SSE stream| Notification

    %% ===== Coordination → Data Layer =====
    Teller -->|查詢帳戶| Account
    Teller -->|執行交易| Transaction
    Exchange -->|查詢帳戶| Account
    Exchange -->|執行交易| Transaction
    Exchange -->|查詢匯率| ExchangeRate

    %% ===== Data Layer → Infrastructure =====
    UserSvc --> PG
    Account --> PG
    Transaction --> PG
    AnalyticsProcessor --> PG
    AnalyticsQuery --> PG
    Notification --> PG

    %% ===== Kafka Event Flows =====
    Transaction -.->|publish events| Kafka
    Teller -.->|publish events| Kafka
    Exchange -.->|publish events| Kafka
    Kafka -.->|consume| AnalyticsProcessor
    Kafka -.->|consume| Notification

    %% ===== Observability Flows =====
    GW -.->|traces| OTel
    Teller -.->|traces| OTel
    Exchange -.->|traces| OTel
    UserSvc -.->|traces| OTel
    Account -.->|traces| OTel
    Transaction -.->|traces| OTel
    OTel -.-> Tempo
    OTel -.-> Loki
    OTel -.-> Prometheus
    Grafana -.-> Tempo
    Grafana -.-> Loki
    Grafana -.-> Prometheus

    %% ===== Styles =====
    classDef critical fill:#ff6b6b,stroke:#c0392b,color:#fff
    classDef coordination fill:#74b9ff,stroke:#2980b9,color:#fff
    classDef data fill:#a29bfe,stroke:#6c5ce7,color:#fff
    classDef infra fill:#fdcb6e,stroke:#f39c12,color:#333
    classDef event fill:#55efc4,stroke:#00b894,color:#333
    classDef gateway fill:#636e72,stroke:#2d3436,color:#fff
    classDef observability fill:#dfe6e9,stroke:#b2bec3,color:#333

    class Transaction critical
    class Teller,Exchange coordination
    class UserSvc,Account,ExchangeRate data
    class PG,Kafka infra
    class AnalyticsProcessor,AnalyticsQuery,Notification event
    class GW,Dashboard gateway
    class OTel,Tempo,Loki,Prometheus,Grafana observability
```

## Service Dependency Matrix

| Service | Type | Port | DB | Kafka | JWT | Calls (downstream) |
|---------|------|------|----|-------|-----|---------------------|
| api-gateway | Gateway | 8080 | - | - | Yes | Routes to all services |
| dashboard | Frontend | 80 | - | - | - | api-gateway |
| teller-service | Coordination | 8083 | - | Pub | - | account-service, transaction-service |
| exchange-service | Coordination | 8085 | - | Pub | - | account-service, transaction-service, exchange-rate-service |
| user-service | Data Layer | 8080 | Yes | - | Yes | - |
| account-service | Data Layer | 8081 | Yes | - | - | - |
| **transaction-service** | **Data Layer** | **8082** | **Yes** | **Pub** | - | - |
| exchange-rate-service | Data Layer | 8084 | - | - | - | - |
| analytics-processor | Event-Driven | 8087 | Yes | Sub | - | - |
| analytics-query | Event-Driven | 8088 | Yes | - | - | - |
| notification-service | Event-Driven | 8089 | Yes | Sub | - | - |

## Critical Paths

```mermaid
graph LR
    subgraph Transfer["轉帳流程"]
        T1[API Gateway] --> T2[teller-service]
        T2 --> T3[account-service]
        T2 --> T4[transaction-service]
        T4 --> T5[(PostgreSQL)]
    end
```

```mermaid
graph LR
    subgraph Deposit["存款/提款流程"]
        D1[API Gateway] --> D2[teller-service]
        D2 --> D3[account-service]
        D2 --> D4[transaction-service]
        D4 --> D5[(PostgreSQL)]
    end
```

```mermaid
graph LR
    subgraph FX["換匯流程"]
        F1[API Gateway] --> F2[exchange-service]
        F2 --> F3[exchange-rate-service]
        F2 --> F4[account-service]
        F2 --> F5[transaction-service]
        F5 --> F6[(PostgreSQL)]
    end
```

```mermaid
graph LR
    subgraph Notify["即時通知流程 — async"]
        N1[transaction-service] -.->|event| N2[Kafka]
        N2 -.-> N3[notification-service]
        N3 -.->|SSE| N4[Browser]
    end
```

## Key Architectural Rules

1. **All balance modifications MUST go through Transaction Service.** Coordination services orchestrate by calling data layer services; they never write directly to the database.
2. **Coordination services are stateless.** They hold no database connections and only call downstream data layer services via HTTP.
3. **Event-driven services are eventually consistent.** Analytics and notifications consume Kafka events asynchronously; their failure does not affect core transaction flows.
4. **API Gateway is the single entry point.** JWT validation happens here; downstream services receive `X-User-ID` header.

## Failure Impact Analysis

| Component | Failure Impact | Blast Radius |
|-----------|---------------|--------------|
| **PostgreSQL** | All DB-dependent services fail (6 services) | Critical — full system |
| **transaction-service** | All write operations fail (teller, exchange) | Critical — all mutations |
| **API Gateway** | No external access to any service | Critical — full system (external) |
| **Kafka** | Event-driven services lose data flow; core transactions unaffected | High — analytics + notifications |
| **account-service** | All coordination services fail (cannot query accounts) | High — all business flows |
| **user-service** | No new logins; existing JWT tokens still work | Medium |
| **exchange-rate-service** | Only exchange flow fails | Low — isolated |
| **analytics-processor** | Analytics data stops updating | Low — no user impact |
| **notification-service** | No real-time push notifications | Low — no user impact |

## Infrastructure (Kubernetes)

- All services deployed as **Deployment** (replicas: 1)
- PostgreSQL and Kafka deployed as **StatefulSet** (replicas: 1)
- Gateway: Kubernetes Gateway API with Cilium
- All services expose health probes: `/actuator/health/readiness` and `/actuator/health/liveness`
- OpenTelemetry: Manual SDK instrumentation + W3C TraceContext propagation
