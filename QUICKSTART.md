# Lite Bank Demo - Quick Start Guide

## 概述

這是一個展示 OpenTelemetry 完整可觀測性的微服務銀行系統示範專案，包含：
- 分散式追蹤（Distributed Tracing）with Grafana Tempo
- 日誌聚合（Log Aggregation）with Grafana Loki
- 指標收集（Metrics Collection）with Prometheus
- SAGA 補償模式（SAGA Compensation Pattern）
- Chaos Engineering with Chaos Mesh

## 前置需求

- Docker Desktop 或 Docker Engine (v20.10+)
- Docker Compose (v2.0+)
- 可用記憶體：至少 4GB

## 快速啟動

### 1. 啟動基礎設施

```bash
# 啟動所有基礎設施服務
docker-compose up -d

# 檢查所有服務狀態
docker-compose ps
```

### 2. 驗證服務狀態

所有服務應該顯示為 `Up` 或 `healthy` 狀態：

```bash
✅ litebank-postgres       (Healthy) - 資料庫
✅ litebank-kafka          (Healthy) - 訊息佇列
✅ litebank-tempo          (Up)      - 分散式追蹤
✅ litebank-loki           (Up)      - 日誌聚合
✅ litebank-prometheus     (Up)      - 指標收集
✅ litebank-grafana        (Up)      - 視覺化平台
✅ litebank-otel-collector (Up)      - OpenTelemetry Collector
```

### 3. 訪問服務

| 服務 | URL | 用途 |
|------|-----|------|
| **Grafana** | http://localhost:3000 | 可觀測性視覺化平台 (admin/admin) |
| **Prometheus** | http://localhost:9090 | 指標查詢介面 |
| **Tempo** | http://localhost:3200 | Trace 查詢 API |
| **PostgreSQL** | localhost:5432 | 資料庫 (litebank_user/litebank_pass) |
| **Kafka** | localhost:9092 | 訊息佇列 |

### 4. 驗證資料庫

```bash
# 查看資料庫 tables
docker exec litebank-postgres psql -U litebank_user -d litebank -c "\dt"

# 查看測試使用者
docker exec litebank-postgres psql -U litebank_user -d litebank -c "SELECT * FROM users;"

# 查看測試帳戶
docker exec litebank-postgres psql -U litebank_user -d litebank -c "SELECT user_id, currency, balance FROM accounts;"
```

**測試資料：**
- 使用者：alice, bob, charlie（密碼：password123）
- Alice 帳戶：USD $1000, EUR €500, TWD $30,000
- Bob 帳戶：USD $2000, JPY ¥100,000
- Charlie 帳戶：USD $500, EUR €1000

## 資料庫 Schema

### Tables 結構

1. **users** - 使用者認證
   - user_id (PK)
   - username, email (unique)
   - password_hash (bcrypt)

2. **accounts** - 銀行帳戶
   - account_id (PK)
   - user_id (FK → users)
   - currency, balance
   - status (ACTIVE/FROZEN/CLOSED)

3. **transactions** - 交易記錄（Append-Only）
   - transaction_id (PK)
   - account_id (FK → accounts)
   - transaction_type, amount, currency
   - balance_after
   - trace_id (for observability)
   - metadata (JSONB)

4. **saga_executions** - SAGA 執行狀態
   - saga_id (PK)
   - transaction_id (FK → transactions)
   - current_step, status
   - compensate_from_step
   - error_message, metadata

## Grafana 設定

Grafana 已預先配置以下 Data Sources：

### 1. **Tempo** (Distributed Tracing)
- URL: http://tempo:3200
- 支援 trace ↔ log 雙向跳轉
- 支援 trace ↔ metric exemplar 跳轉

### 2. **Loki** (Log Aggregation)
- URL: http://loki:3100
- 支援 log ↔ trace 雙向跳轉
- 透過 trace_id 關聯

### 3. **Prometheus** (Metrics)
- URL: http://prometheus:9090
- 啟用 Exemplar 功能
- 支援 metric → trace 跳轉

## OpenTelemetry Collector

OTel Collector 已配置：
- **Receivers:** OTLP gRPC (4317), OTLP HTTP (4318)
- **Sampling:** 100% (Phase 1 demo)
- **Exporters:**
  - Traces → Tempo
  - Logs → Loki
  - Metrics → Prometheus

微服務應該發送遙測資料到：
- `http://otel-collector:4318` (HTTP)
- `otel-collector:4317` (gRPC)

## 停止與清理

```bash
# 停止所有服務
docker-compose down

# 停止並刪除所有資料（包含資料庫）
docker-compose down -v

# 查看日誌
docker-compose logs -f [service-name]
```

## 下一步

現在基礎設施已經準備好，你可以：

1. **開發微服務** - 實作 User Service, Account Service, Transaction Service
2. **整合 OpenTelemetry SDK** - 在微服務中加入 trace, log, metrics instrumentation
3. **測試 SAGA Pattern** - 實作跨幣別轉帳的 SAGA 流程
4. **驗證可觀測性** - 在 Grafana Tempo 中視覺化完整的 trace

## 故障排除

### PostgreSQL 無法啟動
```bash
# 檢查 logs
docker logs litebank-postgres

# 確認 port 5432 未被占用
lsof -i :5432
```

### Kafka 無法啟動
```bash
# 檢查 logs
docker logs litebank-kafka

# 確認 port 9092 未被占用
lsof -i :9092
```

### OTel Collector 啟動失敗
```bash
# 檢查配置語法
docker logs litebank-otel-collector

# 驗證配置檔案
cat otel/otel-collector-config.yaml
```

### Grafana 無法連接 Data Sources
1. 進入 Grafana: http://localhost:3000
2. 前往 Configuration → Data Sources
3. 測試每個 Data Source 連線
4. 確認所有服務都在同一個 Docker network (`litebank`)

## 技術架構

```
┌─────────────────────────────────────────────────────┐
│                  Microservices Layer                │
│   (User, Account, Transaction, Transfer Services)  │
└────────────────┬────────────────────────────────────┘
                 │ OTLP (HTTP/gRPC)
                 ↓
┌─────────────────────────────────────────────────────┐
│            OpenTelemetry Collector                  │
│  (Receive, Process, Export Telemetry Data)          │
└─────┬──────────────┬──────────────┬─────────────────┘
      │              │              │
      ↓              ↓              ↓
  ┌───────┐    ┌────────┐    ┌──────────┐
  │ Tempo │    │  Loki  │    │Prometheus│
  │(Trace)│    │ (Logs) │    │(Metrics) │
  └───┬───┘    └───┬────┘    └────┬─────┘
      │            │              │
      └────────────┴──────────────┘
                   │
                   ↓
            ┌─────────────┐
            │   Grafana   │
            │(Visualization)│
            └─────────────┘
```

## 資源

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana Tempo Docs](https://grafana.com/docs/tempo/)
- [Grafana Loki Docs](https://grafana.com/docs/loki/)
- [Prometheus Docs](https://prometheus.io/docs/)

---

**專案狀態：** Phase 1 - 基礎設施完成 ✅

**下一個 Story：** User Service with JWT Authentication
