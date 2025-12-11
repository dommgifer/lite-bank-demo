---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments:
  - 'docs/analysis/product-brief-lite-bank-demo-2025-12-04.md'
workflowType: 'architecture'
lastStep: 8
status: 'complete'
completedAt: '2025-12-09'
project_name: 'lite-bank-demo'
user_name: '主人'
date: '2025-12-04'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

lite-bank-demo 是一個技術展示專案,核心目標是展示現代可觀測性最佳實踐。從架構角度來看,功能需求分為兩大類:

**1. 銀行業務功能(用於產生真實複雜度):**
- **帳戶管理**: 多幣別帳戶支援(TWD, USD, JPY),帳戶餘額查詢與管理,資料持久化於 PostgreSQL
- **存提款操作**: 單一帳戶的金額增減,需要原子性操作
- **轉帳功能**: 跨帳戶轉帳(含跨幣別),採用 SAGA 分散式事務模式
  - 6 步驟流程:凍結來源帳戶 → 查詢匯率 → 扣款 → 入帳 → 記錄交易 → 發送通知
  - 完整補償邏輯(Compensating Transactions)
  - **關鍵架構決策**: 必須同步處理(非異步),符合金融業務需求
- **換匯功能**: 幣別兌換操作,匯率查詢(模擬外部 API)
- **交易歷史查詢**: 完整的交易記錄與查詢功能
- **使用者認證**: JWT 認證(簡化版,非完整 RBAC)

**2. 可觀測性功能(展示核心價值):**
- **log ↔ trace ↔ metric 三向跳轉**: 從任一可觀測性支柱跳轉到其他支柱
- **SAGA 流程完整追蹤**: 分散式事務的每個步驟在 Grafana Tempo 清楚可視化
- **Chaos Mesh 故障場景**: 真實故障注入(NetworkChaos, PodChaos, StressChaos)
- **前端到後端完整鏈路**: React UI → API Gateway → 微服務 → Kafka → WebSocket 通知
- **Kafka 異步通訊追蹤**: 跨訊息佇列的 trace context propagation

**Non-Functional Requirements:**

這些 NFRs 將直接驅動架構決策:

1. **可觀測性完整性(最高優先級)**
   - 跨語言 trace propagation 無缺口(Java SpringBoot → Python → Quarkus → Kafka)
   - 所有服務必須手動實作 OpenTelemetry SDK
   - 自定義 span attributes 包含業務語義(account.id, transaction.amount 等)
   - 統一 trace context 自動關聯 logs, traces, metrics

2. **技術真實性**
   - 金融交易同步處理(轉帳/換匯不能異步)
   - Kafka 異步僅用於通知服務(簡訊/Email/推播)
   - 真實故障場景(Chaos Mesh),非刻意設計的 demo 錯誤
   - 生產等級架構(Kafka KRaft, K8s 多 Namespace, PostgreSQL)

3. **Demo 展示需求**
   - 視覺化衝擊 > 技術細節教學
   - 一鍵部署所有服務
   - 即時觸發 Chaos 場景(`kubectl apply -f chaos-scenarios/`)
   - UI 美觀且操作流暢(React + Material-UI)

4. **效能與可靠性(Phase 1 基本要求)**
   - 基本微服務架構應可承受 Demo 演示負載
   - Level 2 錯誤分類處理(業務錯誤、外部依賴錯誤、系統錯誤)
   - 補償邏輯確保資料一致性

**Scale & Complexity:**

- **Primary domain**: 全端(Full-stack)微服務系統 - 前端 React + 10 個後端微服務 + K8s + 可觀測性工具鏈
- **Complexity level**: 高複雜度(High Complexity)
  - 10 個微服務跨 3 種語言(Java SpringBoot, Java Quarkus, Python)
  - 分散式事務(SAGA 模式)
  - 完整可觀測性實作(非 auto-instrumentation)
  - Chaos Engineering 整合
  - 前端到後端完整追蹤
- **Estimated architectural components**: 25+ 個主要元件
  - 10 個業務微服務
  - 5 個可觀測性元件(Grafana, Tempo, Loki, Prometheus, OTel Collector)
  - 1 個訊息佇列(Kafka KRaft)
  - 1 個混沌工程平台(Chaos Mesh)
  - 1 個前端應用(React + TypeScript)
  - 1 個資料庫(PostgreSQL)
  - K8s 基礎設施(4 個 Namespace)

### Technical Constraints & Dependencies

**技術約束:**

1. **框架選擇約束**
   - Account Service 和 Currency Exchange Service 必須使用 Quarkus(展示 auto-instrumentation 盲點)
   - Transfer Service 必須使用 Python(作為 SAGA 編排器,展示跨語言 propagation)
   - 其他服務使用 Java SpringBoot(主流技術棧)

2. **可觀測性工具鏈固定**
   - Grafana Stack: Tempo(traces) + Loki(logs) + Prometheus(metrics)
   - OpenTelemetry Collector(統一資料收集)
   - 必須手動 SDK instrumentation,不能使用 auto-instrumentation

3. **基礎設施約束**
   - Kubernetes 單叢集,4 個 Namespace(banking-services, observability, messaging, chaos)
   - Kafka KRaft mode(無 ZooKeeper)
   - PostgreSQL 作為唯一資料庫

4. **Phase 1 範圍約束(明確不做)**
   - 不實作多租戶(Multi-tenancy)
   - 不串接真實外部 API(匯率查詢模擬即可)
   - 不實作真實簡訊/Email(使用 WebSocket 推播通知取代)
   - 不實作完整 RBAC(簡化認證即可)
   - 不實作 Level 3 錯誤處理(重試、斷路器、降級)
   - 不進行效能測試/負載測試

**關鍵依賴:**

1. **OpenTelemetry SDK**
   - Java: `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-instrumentation-*`
   - Python: `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-instrumentation-*`
   - React: `@opentelemetry/sdk-trace-web`, `@opentelemetry/instrumentation-*`

2. **Grafana Stack**
   - Tempo 作為 trace backend
   - Loki 作為 log aggregation
   - Prometheus 作為 metrics backend
   - Grafana 作為統一視覺化介面

3. **Kafka**
   - Kafka 4.0+ (KRaft mode)
   - Topic: `banking.notifications`

4. **Chaos Mesh**
   - NetworkChaos, PodChaos, StressChaos CRDs

### Cross-Cutting Concerns Identified

以下關注點將影響多個架構元件,需要統一的設計決策:

**1. Trace Context Propagation(最關鍵)**
- **影響範圍**: 所有 10 個微服務 + 前端 + Kafka + WebSocket
- **架構挑戰**:
  - 跨語言 trace context 傳遞(Java ↔ Python)
  - 跨框架 trace context 傳遞(SpringBoot ↔ Quarkus)
  - HTTP headers 注入與提取(`traceparent`, `tracestate`)
  - Kafka message headers 的 trace context propagation
  - 前端 Browser SDK 生成 trace_id 傳遞到後端
  - **WebSocket trace context 傳遞**(架構決策:保留完整鏈路追蹤)
- **架構決策**:
  - 統一的 trace context 格式、propagator 配置、context 提取策略
  - WebSocket message format 包含 `_trace` 欄位:
    ```json
    {
      "messageId": "msg-001",
      "message": "轉帳成功!已從帳戶 TWD-001 轉出 1000 元",
      "_trace": {
        "traceId": "abc123",
        "spanId": "def456"
      }
    }
    ```

**2. 自定義 Span Attributes(業務語義標註)**
- **影響範圍**: 所有微服務
- **架構挑戰**:
  - 定義統一的業務語義 attributes 命名規範
  - 確保所有服務遵循相同的標註標準
  - 跨語言同步 attributes 定義
- **範例 attributes**:
  - 帳戶:`account.id`, `account.currency`, `account.balance`
  - 交易:`transaction.id`, `transaction.amount`, `transaction.type`
  - 匯率:`exchange.from_currency`, `exchange.to_currency`, `exchange.rate`
  - SAGA:`saga.id`, `saga.step`, `saga.status`
  - 錯誤:`error.type`, `error.category`, `error.message`
- **架構決策**:
  - 建立中心化規範文件:`docs/opentelemetry-conventions.md`
  - 各語言手動實作常量類別(Java: `OTelAttributes.java`, Python: `otel_attributes.py`, TypeScript: `otelAttributes.ts`)
  - Phase 2 可選:OpenAPI/YAML schema + code generation 自動化

**3. SAGA 錯誤處理與補償邏輯**
- **影響範圍**: Transfer Service(編排器) + Account/Exchange Rate/Transaction Services(參與者)
- **架構挑戰**:
  - 定義統一的 SAGA 步驟狀態(進行中、成功、失敗、已補償)
  - 補償邏輯的冪等性設計
  - 錯誤在 trace span 中的標註策略
  - Timeout 處理機制
  - **SAGA 狀態持久化**(架構決策:使用 PostgreSQL 狀態表)
- **架構決策**:
  - SAGA 狀態表設計:
    ```sql
    CREATE TABLE saga_executions (
      saga_id UUID PRIMARY KEY,
      transaction_id VARCHAR(50),
      current_step INT,
      status VARCHAR(20),
      compensate_from_step INT,
      created_at TIMESTAMP,
      updated_at TIMESTAMP
    );
    ```
  - Transfer Service 每步驟完成後更新狀態
  - Pod 重啟後可從狀態表恢復(Phase 2 實作)
  - Phase 1 接受簡化:SAGA 不支援中途重啟恢復

**4. Level 2 錯誤分類處理**
- **影響範圍**: 所有微服務
- **三層錯誤分類**:
  - 業務錯誤(可預期):`InsufficientBalanceException`, `AccountNotFoundException`, `InvalidAmountException`
  - 外部依賴錯誤:`ExchangeRateTimeoutException`, 第三方服務不可用
  - 系統錯誤(未預期):未分類異常
- **架構決策**:
  - 統一的異常類別階層:`exception-hierarchy.md`
  - 錯誤碼系統(可選)
  - Span 標註方式:
    ```java
    span.setAttribute("error.type", "InsufficientBalance");
    span.setAttribute("error.category", "business");
    span.setAttribute("error.message", "餘額不足");
    ```

**5. K8s 部署與配置**
- **影響範圍**: 所有服務 + 基礎設施元件
- **架構挑戰**:
  - 4 個 Namespace 的網路隔離與 Service Discovery
  - ConfigMap / Secret 管理策略
  - 環境變數注入(OTel Collector endpoint, Kafka bootstrap servers 等)
  - Resource limits 與 requests 設定
- **架構決策**:
  - Helm Charts 或 Kustomize 結構設計
  - 一鍵部署腳本
  - 統一的配置管理策略

**6. 資料持久化與 Schema 設計**
- **影響範圍**: Account/User/Transaction Services + PostgreSQL
- **架構挑戰**:
  - 多幣別帳戶的資料模型設計
  - 交易記錄與帳戶餘額的一致性維護
  - 資料庫連接池管理(多個服務共享同一 PostgreSQL)
  - SAGA 狀態表整合
- **架構決策**:
  - 資料庫 schema 設計
  - 連接池配置策略
  - 資料初始化腳本

**7. WebSocket 推播通知**
- **影響範圍**: Notification Service + React 前端
- **架構挑戰**:
  - WebSocket 連接管理(連接建立、心跳、重連)
  - Trace context 在 WebSocket 訊息中的傳遞
  - 前端訂閱機制(哪些使用者收到哪些通知)
- **架構決策**:
  - WebSocket 協定設計(包含 trace context)
  - 訊息格式定義
  - Trace propagation 策略(保留完整鏈路)

**8. Chaos Engineering 測試策略**
- **影響範圍**: 所有微服務 + Demo 展示流程
- **架構挑戰**:
  - 定義 Chaos 場景的預期行為
  - 平衡「確定成功」與「探索性發現」
  - Demo 流程設計與風險管理
- **架構決策**:
  - **混合策略**:
    - 1-2 個有明確預期行為的場景(確保 Demo 流暢)
      - 例如:NetworkChaos Exchange Rate 延遲 → timeout → 補償邏輯
    - 2-3 個探索性場景(展示混沌工程真正價值)
      - 例如:StressChaos CPU 100% → 觀察實際系統行為
  - 每個場景包含:觸發方式、預期行為(如有)、Demo 腳本、恢復方式
  - 文件:`chaos-scenarios/README.md`

### Architecture Analysis Summary

**專案複雜度評估:**
- **高複雜度** - 10 個微服務,3 種語言,分散式事務,完整可觀測性
- **核心挑戰** - Trace context propagation 無缺口、SAGA 狀態管理、真實故障場景展示
- **獨特價值** - 市面上少見的完整可觀測性 demo(前端 → 後端 → Kafka → WebSocket)

**關鍵架構決策已確定:**
1. ✅ SAGA 狀態持久化(PostgreSQL 狀態表)
2. ✅ WebSocket 保留 trace context(完整鏈路追蹤)
3. ✅ 中心化 Span attributes 規範(`docs/opentelemetry-conventions.md`)
4. ✅ Chaos 混合測試策略(確定場景 + 探索性場景)
5. ✅ Level 2 錯誤分類處理(業務/外部依賴/系統)

**下一步:** 開始進行具體的架構設計決策(技術選型、元件設計、資料流設計等)

## Core Architectural Decisions

_本章節記錄所有關鍵技術選型與架構設計決策,包含決策理由、技術版本、影響範圍與實作指引。_

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
以下決策必須在實作前確定,否則無法開始開發:

1. ✅ **資料庫遷移工具**: Flyway - 確保 schema 版本控制與多環境一致性
2. ✅ **微服務資料庫策略**: Shared Schema + 開發紀律文件 - 平衡 Phase 1 簡化與未來擴展性
3. ✅ **API 版本控制方式**: URL Path Versioning - 明確的 API 演進路徑
4. ✅ **錯誤處理格式**: 統一 JSON 格式 + 數字錯誤碼(ERR_001) - 跨服務一致的錯誤處理
5. ✅ **K8s 部署工具**: Helm Charts - 一鍵部署與配置管理
6. ✅ **OTel Collector 採樣策略**: 彈性配置(預設 Full Sampling) - 支援 Demo 與壓測場景切換

**Important Decisions (Shape Architecture):**
這些決策顯著影響架構設計,但可在實作過程中微調:

1. ✅ **前端狀態管理**: React Context API - 簡化狀態管理,適合 MVP 規模
2. ✅ **WebSocket 客戶端**: Socket.io-client - 自動重連與心跳機制
3. ✅ **日誌格式**: Structured JSON - 支援 log ↔ trace 跳轉
4. ✅ **Metrics 範圍**: 僅應用層級 metrics - 聚焦系統健康度,避免業務 metrics 混淆

**Deferred Decisions (Post-MVP):**
Phase 1 明確不做,Phase 2 再評估:

1. ⏸️ **Separate Schemas 遷移**: Phase 1 使用 Shared Schema,Phase 2 評估是否需要拆分
2. ⏸️ **業務 metrics**: Phase 1 僅應用層級 metrics,Phase 2 可選擇性新增業務指標
3. ⏸️ **SAGA 中途恢復**: Phase 1 不支援 Pod 重啟後 SAGA 恢復,Phase 2 實作狀態恢復邏輯
4. ⏸️ **Ingress Controller**: Phase 1 可選安裝 Nginx Ingress,Phase 2 評估生產環境需求

---

### 1. Data Architecture

#### 1.1 Database Migration Tool

**Decision: Flyway**

**Rationale:**
- Java 生態系統原生支援(SpringBoot/Quarkus 都有 Flyway 整合)
- SQL-based migrations 易於審查與版本控制
- 支援多環境配置(dev, staging, prod)
- 清晰的遷移歷史追蹤(`flyway_schema_history` 表)

**Version:** Flyway 10.x (latest stable 2024)

**Implementation Guidance:**
- Migration scripts 放置於 `src/main/resources/db/migration/`
- 命名規範:`V{version}__{description}.sql`(例如:`V1__create_accounts_table.sql`)
- 初始 schema 包含:
  - `accounts` 表(多幣別帳戶)
  - `transactions` 表(交易記錄)
  - `saga_executions` 表(SAGA 狀態)
  - `users` 表(使用者資訊)
- 每個微服務獨立執行 Flyway migration(避免權限問題)

**Affects:** Account Service, Transaction Service, User Service, Transfer Service

---

#### 1.2 Multi-Currency Account Design

**Decision: Single Table with Currency Column**

**Database Schema:**
```sql
CREATE TABLE accounts (
    account_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,  -- TWD, USD, JPY
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_balance CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);
```

**Rationale:**
- 簡化查詢邏輯(避免多表 JOIN 或 UNION)
- Currency 作為查詢條件時可使用索引加速
- 符合 KISS 原則(Phase 1 簡化設計)
- 未來可輕鬆遷移至 JSON 欄位儲存多幣別餘額(Phase 2 可選)

**Account ID Format:** `{CURRENCY}-{SEQUENTIAL_NUMBER}`(例如:`TWD-001`, `USD-002`)

**Affects:** Account Service, Transfer Service, Currency Exchange Service

---

#### 1.3 Microservices Database Strategy

**Decision: Shared Schema with Development Discipline**

**Strategy:**
- **Phase 1**: 所有微服務共享同一 PostgreSQL 實例與 Schema
- **Access Control**: 透過開發紀律文件(`docs/database-access-rules.md`)定義服務-表格存取權限
- **Future Path**: Phase 2 可評估遷移至 Separate Schemas 或 Database per Service

**Development Discipline Document Requirements:**

`docs/database-access-rules.md` 必須包含:

1. **服務-表格存取矩陣**:
   | Service | accounts | transactions | users | saga_executions | exchange_rates |
   |---------|----------|--------------|-------|-----------------|----------------|
   | Account Service | RW | R | R | - | - |
   | Transaction Service | R | RW | R | - | - |
   | Transfer Service | R | R | R | RW | R |
   | User Service | - | - | RW | - | - |
   | Currency Exchange Service | - | - | - | - | RW |

2. **Code Review 檢查清單**:
   - ❌ 禁止跨服務直接存取其他服務的主表(例如:Transaction Service 不能直接寫入 `accounts`)
   - ✅ 跨服務資料存取必須透過 API 呼叫
   - ✅ 唯讀存取允許(例如:Transfer Service 可讀取 `accounts` 驗證餘額)

3. **資料庫連接池配置**:
   - 每個服務獨立配置連接池(避免資源競爭)
   - 建議連接池大小:`min=2, max=10`(Phase 1 Demo 規模)

**Rationale:**
- Phase 1 聚焦可觀測性展示,避免過早優化
- 開發紀律文件作為團隊協作契約
- Code Review 強制執行存取規則
- 保留未來遷移彈性

**Affects:** All microservices accessing PostgreSQL

---

#### 1.4 SAGA State Persistence

**Decision: PostgreSQL State Table**

**Database Schema:**
```sql
CREATE TABLE saga_executions (
    saga_id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    current_step INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,  -- IN_PROGRESS, COMPLETED, FAILED, COMPENSATED
    compensate_from_step INT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saga_transaction_id ON saga_executions(transaction_id);
CREATE INDEX idx_saga_status ON saga_executions(status);
```

**SAGA Steps Tracking:**
Transfer Service 在每個步驟完成後更新 `current_step` 與 `status`:

| Step | Description | current_step | Status on Success | Status on Failure |
|------|-------------|--------------|-------------------|-------------------|
| 1 | Freeze source account | 1 | IN_PROGRESS | FAILED |
| 2 | Query exchange rate | 2 | IN_PROGRESS | FAILED |
| 3 | Debit source account | 3 | IN_PROGRESS | FAILED |
| 4 | Credit target account | 4 | IN_PROGRESS | FAILED |
| 5 | Record transaction | 5 | IN_PROGRESS | FAILED |
| 6 | Send notification | 6 | COMPLETED | FAILED |
| Compensation | Rollback changes | varies | COMPENSATED | FAILED |

**Phase 1 Limitation:**
- **不支援 Pod 重啟後 SAGA 恢復**(簡化實作)
- Transfer Service 重啟後,進行中的 SAGA 視為失敗
- Phase 2 可實作:啟動時掃描 `IN_PROGRESS` 狀態的 SAGA 並恢復執行

**Rationale:**
- 狀態持久化確保 SAGA 流程可追蹤與審計
- 支援 Grafana 查詢 SAGA 執行歷史
- 為 Phase 2 恢復邏輯預留基礎

**Affects:** Transfer Service (SAGA Orchestrator)

---

#### 1.5 Data Initialization

**Decision: SQL Seed Scripts**

**Implementation:**
- Seed data scripts 放置於 `src/main/resources/db/seed/`
- 包含:
  - 測試使用者帳號(至少 3 個使用者,每個使用者擁有 TWD/USD/JPY 各一個帳戶)
  - 初始餘額(例如:TWD 帳戶 10,000 元)
  - 模擬匯率資料(TWD/USD, USD/JPY 等)
- 執行方式:
  - Development 環境:啟動時自動執行
  - Staging/Production 環境:手動執行或透過 CI/CD 控制

**Seed Data Example:**
```sql
-- Users
INSERT INTO users (user_id, username, email, password_hash) VALUES
  ('user-001', 'alice', 'alice@example.com', '$2a$10$...'),
  ('user-002', 'bob', 'bob@example.com', '$2a$10$...');

-- Accounts
INSERT INTO accounts (account_id, user_id, currency, balance) VALUES
  ('TWD-001', 'user-001', 'TWD', 10000.00),
  ('USD-001', 'user-001', 'USD', 500.00),
  ('TWD-002', 'user-002', 'TWD', 20000.00);
```

**Affects:** Account Service, User Service, Currency Exchange Service

---

### 2. Authentication & Security

#### 2.1 Authentication Method

**Decision: Simplified JWT Authentication**

**Implementation:**
- User Service 提供 `/api/v1/auth/login` endpoint 發放 JWT token
- Token 包含:`user_id`, `username`, `exp`(過期時間)
- Token 有效期:24 小時(Demo 演示用)
- **Phase 1 簡化**:不實作 refresh token、token revocation、RBAC

**JWT Payload Example:**
```json
{
  "user_id": "user-001",
  "username": "alice",
  "iat": 1733356800,
  "exp": 1733443200
}
```

**API Gateway Integration:**
- API Gateway(如果實作)驗證 JWT 有效性
- 或由各微服務自行驗證(使用共享的 JWT secret)

**Security Note:**
- Phase 1 為 Demo 專案,密碼使用 bcrypt 雜湊即可
- JWT secret 透過 K8s Secret 管理
- **不實作**:OAuth2, 多因素認證, 權限角色

**Affects:** User Service, API Gateway (if implemented), Frontend

---

### 3. API & Communication Patterns

#### 3.1 API Versioning

**Decision: URL Path Versioning**

**Format:** `/api/v{version}/{resource}`

**Examples:**
- Account Service: `GET /api/v1/accounts/{accountId}`
- Transfer Service: `POST /api/v1/transfers`
- Transaction Service: `GET /api/v1/transactions?accountId={accountId}`

**Rationale:**
- 明確且直觀的版本識別
- 易於監控與追蹤(version 出現在 URL path 中)
- 支援同時運行多版本 API(Phase 2 可選)

**Version Upgrade Strategy:**
- Phase 1 僅實作 v1
- Phase 2 若需 breaking changes,新增 v2 並保留 v1 向後相容
- Grafana 可按 API version 分組追蹤 metrics

**Affects:** All microservices exposing REST APIs

---

#### 3.2 API Documentation

**Decision: OpenAPI 3.0 + Swagger UI**

**Implementation:**
- 每個微服務提供 OpenAPI 規格檔(YAML 或 JSON)
- Swagger UI endpoint: `/api-docs` 或 `/swagger-ui`
- SpringBoot: 使用 `springdoc-openapi-starter-webmvc-ui` 依賴
- Quarkus: 使用 `quarkus-smallrye-openapi` 擴展
- Python (FastAPI): 內建支援 OpenAPI 與 Swagger UI

**Documentation Requirements:**
- 所有 endpoints 包含:
  - Request/Response schemas
  - Example payloads
  - Error responses(包含錯誤碼對照)
  - Trace context headers 說明(`traceparent`)

**Centralized Documentation (Optional):**
- Phase 2 可選:部署單一 Swagger UI aggregator 集中展示所有服務的 API

**Affects:** All microservices

---

#### 3.3 Error Handling Standards

**Decision: Unified JSON Error Response + Numeric Error Codes**

**Error Response Format:**
```json
{
  "error": {
    "code": "ERR_001",
    "type": "INSUFFICIENT_BALANCE",
    "message": "餘額不足,目前餘額 500 元,需要 1000 元",
    "category": "business",
    "details": {
      "account_id": "TWD-001",
      "current_balance": 500,
      "required_amount": 1000
    },
    "traceId": "abc123def456",
    "timestamp": "2024-12-04T10:30:00Z"
  }
}
```

**Error Code Mapping:**
建立錯誤碼對照表於 `docs/error-codes.md`:

| Code | Type | Category | HTTP Status | Description |
|------|------|----------|-------------|-------------|
| ERR_001 | INSUFFICIENT_BALANCE | business | 400 | 帳戶餘額不足 |
| ERR_002 | ACCOUNT_NOT_FOUND | business | 404 | 帳戶不存在 |
| ERR_003 | INVALID_AMOUNT | business | 400 | 金額格式錯誤或為負數 |
| ERR_004 | EXCHANGE_RATE_TIMEOUT | external_dependency | 503 | 匯率查詢服務逾時 |
| ERR_005 | DATABASE_CONNECTION_ERROR | system | 500 | 資料庫連線失敗 |

**Error Categories:**
- `business`: 可預期的業務邏輯錯誤(HTTP 4xx)
- `external_dependency`: 外部依賴服務錯誤(HTTP 503/504)
- `system`: 系統內部未預期錯誤(HTTP 500)

**OpenTelemetry Span Integration:**
所有錯誤必須在 span 中標註:
```java
span.setAttribute("error.code", "ERR_001");
span.setAttribute("error.type", "INSUFFICIENT_BALANCE");
span.setAttribute("error.category", "business");
span.setAttribute("error.message", "餘額不足");
span.setStatus(StatusCode.ERROR, "餘額不足");
```

**Implementation Guidance:**
- 建立統一的 Exception Handler(SpringBoot: `@ControllerAdvice`, FastAPI: `@app.exception_handler`)
- 自訂 Exception 類別繼承體系(參考 `docs/exception-hierarchy.md`)
- 確保所有 API 錯誤回應遵循此格式

**Affects:** All microservices

---

#### 3.4 Rate Limiting Strategy

**Decision: Phase 1 不實作,Phase 2 評估**

**Rationale:**
- Phase 1 為 Demo 專案,不預期真實流量
- 聚焦可觀測性展示,避免功能過載
- Phase 2 若需壓測場景,可實作 API Gateway 層級的 rate limiting

**Future Implementation Options (Phase 2):**
- Nginx Ingress rate limiting annotations
- API Gateway(如 Kong, Traefik)內建 rate limiting
- Application-level rate limiting(Spring Boot: Resilience4j RateLimiter)

**Affects:** N/A (Phase 1 skipped)

---

#### 3.5 API Gateway Strategy

**Decision: Spring Cloud Gateway 3.1.x (Phase 1 必備)**

**架構定位:**
- **Spring Cloud Gateway 3.1.x** 作為統一 API 入口
- 基於 Spring WebFlux(非阻塞,高效能)
- 與 Java 技術棧一致性高
- 簡化前端配置,單一 endpoint (`http://api-gateway:8080`)

**API Gateway 核心責任:**

1. **路由與聚合**
   - 統一入口,路由請求至對應微服務
   - 基於 URL path 動態路由(`/api/v1/accounts/**` → Account Service)
   - 支援 K8s Service Discovery(透過 `lb://service-name`)

2. **集中式 JWT 驗證**
   - 避免每個微服務重複驗證邏輯
   - JWT secret 統一管理(從 K8s Secret 注入)
   - 驗證後將 `userId` 注入至 header 傳遞給後端

3. **Trace Context 注入**
   - 確保所有請求都有 `traceparent` header
   - 統一 OpenTelemetry instrumentation
   - 自動生成 Request ID(`X-Request-ID`)

4. **CORS 處理**
   - 集中管理跨域策略
   - 前端 React 應用需要的跨域支援
   - 開發環境允許 `localhost:3000`

5. **基本速率限制**
   - 全域限流:100 req/s per IP
   - 保護後端微服務免受 DDoS

**Spring Cloud Gateway 完整配置:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**
        - id: account-service
          uri: lb://account-service
          predicates:
            - Path=/api/v1/accounts/**
        - id: transfer-service
          uri: lb://transfer-service
          predicates:
            - Path=/api/v1/transfers/**
        - id: transaction-service
          uri: lb://transaction-service
          predicates:
            - Path=/api/v1/transactions/**
        - id: deposit-withdrawal-service
          uri: lb://deposit-withdrawal-service
          predicates:
            - Path=/api/v1/deposits/**, /api/v1/withdrawals/**
        - id: currency-exchange-service
          uri: lb://currency-exchange-service
          predicates:
            - Path=/api/v1/exchanges/**
        - id: exchange-rate-service
          uri: lb://exchange-rate-service
          predicates:
            - Path=/api/v1/rates/**
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:3000"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders: "*"
            allowCredentials: true
```

**JWT Secret 管理策略:**

```yaml
# K8s Secret for JWT
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
  namespace: lite-bank-services
type: Opaque
data:
  secret: <base64-encoded-secret>
```

**JWT Secret 輪換策略:**
- **輪換週期**: 每 90 天輪換一次
- **Grace Period**: 新舊密鑰共存 24 小時,確保平滑過渡
- **緊急輪換**: 發現洩漏時立即執行
- **管理工具**: K8s Secret + External Secrets Operator (Phase 2 可選)

**Rationale:**
- **統一入口**: 前端只需配置一個 API endpoint,簡化部署
- **集中驗證**: JWT 驗證邏輯集中,減少微服務重複代碼
- **安全性**: CORS 與 rate limiting 集中管理,降低配置錯誤風險
- **可觀測性**: 統一注入 trace context,確保完整鏈路追蹤
- **技術一致性**: Spring Cloud Gateway 與 SpringBoot 生態系統無縫整合

**Alternative Options Considered:**
- **Nginx**: 輕量級,但缺乏 Service Discovery 支援,需手動配置 upstream
- **Kong**: 功能強大,但 Java 技術棧一致性較低,學習成本高
- **Traefik**: Cloud-native,但配置複雜度高,不如 Spring 生態系統熟悉

**Performance Characteristics:**
- 單機處理能力: 10,000+ req/s
- 記憶體占用: ~200-300MB
- 延遲增加: ~5-10ms (可接受)

**Affects:** Frontend (單一 API endpoint), All microservices (移除 JWT 驗證邏輯), Deployment strategy (新增 API Gateway 服務)

---

#### 3.6 Internal Service Communication

**Decision: Synchronous HTTP + Asynchronous Kafka (Notification Only)**

**Communication Patterns:**

1. **Synchronous HTTP** (Primary):
   - 所有業務邏輯通訊使用 HTTP REST API
   - Transfer Service → Account Service: HTTP
   - Transfer Service → Currency Exchange Service: HTTP
   - Transfer Service → Transaction Service: HTTP
   - **Trace context propagation**: HTTP headers (`traceparent`, `tracestate`)

2. **Asynchronous Kafka** (Notification Only):
   - Transfer Service → Kafka Topic `banking.notifications` → Notification Service
   - **僅用於通知服務**(簡訊/Email/推播模擬)
   - **Trace context propagation**: Kafka message headers

**Rationale:**
- 金融交易必須同步處理(符合 ACID 需求)
- Kafka 異步僅用於非關鍵路徑的通知功能
- 簡化架構,避免過度設計

**Kafka Topic Design:**
```yaml
Topic: banking.notifications
Partitions: 3
Replication Factor: 1 (single-node Kafka for Demo)
Message Format:
  {
    "notificationId": "notif-001",
    "userId": "user-001",
    "type": "TRANSFER_SUCCESS",
    "message": "轉帳成功!已從帳戶 TWD-001 轉出 1000 元",
    "_trace": {
      "traceId": "abc123",
      "spanId": "def456"
    },
    "timestamp": "2024-12-04T10:30:00Z"
  }
```

**Affects:** Transfer Service, Notification Service, Kafka

---

### 4. Frontend Architecture

#### 4.1 State Management

**Decision: React Context API**

**Rationale:**
- Phase 1 MVP 規模適合 Context API(避免 Redux 複雜度)
- 狀態範圍有限:使用者資訊、帳戶列表、交易歷史
- 簡化學習曲線與開發速度

**Context Structure:**
```typescript
// AuthContext: 使用者登入狀態與 JWT token
interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

// AccountContext: 帳戶資訊與餘額
interface AccountContextType {
  accounts: Account[];
  selectedAccount: Account | null;
  fetchAccounts: () => Promise<void>;
  selectAccount: (accountId: string) => void;
}

// NotificationContext: WebSocket 推播通知
interface NotificationContextType {
  notifications: Notification[];
  connected: boolean;
}
```

**Phase 2 Migration Path:**
- 若狀態管理變複雜,可遷移至 Zustand 或 Redux Toolkit
- Context API 程式碼易於重構至 Zustand(狀態結構相似)

**Affects:** React Frontend

---

#### 4.2 WebSocket Client

**Decision: Socket.io-client**

**Rationale:**
- 自動重連與心跳機制(避免手動實作 native WebSocket 重連邏輯)
- 跨瀏覽器相容性佳
- 與後端 Socket.io server(Java/Python 實作)配對

**Implementation:**
```typescript
import { io, Socket } from 'socket.io-client';

const socket: Socket = io('ws://notification-service:8080', {
  auth: {
    token: jwtToken  // JWT 認證
  },
  transports: ['websocket']
});

socket.on('connect', () => {
  console.log('WebSocket connected');
});

socket.on('notification', (data) => {
  // data 包含 _trace 欄位
  console.log('Received notification:', data);
  // 前端可透過 traceId 查詢 Grafana Tempo
});
```

**Trace Context Propagation:**
- Notification Service 發送的 WebSocket 訊息包含 `_trace` 欄位
- 前端接收後可在 UI 顯示 `traceId` 連結至 Grafana Tempo

**Affects:** React Frontend, Notification Service

---

#### 4.3 UI Component Library

**Decision: Material-UI (MUI) v5**

**Version Specification:**
- **@mui/material: ^5.15.0** (MUI v5 最新穩定版,2024)
- **@mui/icons-material: ^5.15.0**
- **@emotion/react: ^11.11.0** (MUI v5 依賴)
- **@emotion/styled: ^11.11.0**

**Rationale:**
- **React 18 完全相容**: MUI v5 原生支援 React 18
- **TypeScript 支援優秀**: 型別定義完整,開發體驗佳
- **Bundle Size 優化**: Tree-shaking 支援良好,按需載入元件
- **主題系統強大**: 適合 lite-bank-demo 的美觀 UI 需求
- **豐富的元件庫**: Button, Card, TextField, Dialog 等開箱即用
- **社群活躍**: 文件完整,問題快速解決

**Theme Configuration:**

```typescript
// frontend/src/main.tsx
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // 銀行藍
    },
    secondary: {
      main: '#dc004e', // 強調紅
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

root.render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </React.StrictMode>
);
```

**Component Usage Examples:**

```typescript
// frontend/src/components/features/AccountCard.tsx
import { Card, CardContent, Typography, Button } from '@mui/material';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';

export const AccountCard: React.FC<{ account: Account }> = ({ account }) => {
  return (
    <Card sx={{ minWidth: 275, mb: 2 }}>
      <CardContent>
        <AccountBalanceIcon color="primary" />
        <Typography variant="h5" component="div">
          {account.currency} - {account.accountId}
        </Typography>
        <Typography variant="h6" color="text.secondary">
          餘額: ${account.balance.toLocaleString()}
        </Typography>
        <Button size="small" variant="contained">查看詳情</Button>
      </CardContent>
    </Card>
  );
};
```

**Integration with OpenTelemetry:**
- MUI 元件不影響 OpenTelemetry Browser SDK
- 元件事件(onClick, onChange)正常觸發 trace spans
- 元件命名 PascalCase 與 MUI 慣例一致

**Bundle 優化策略:**

1. **Code Splitting (React.lazy):**
```typescript
// frontend/src/routes/AppRoutes.tsx
import { lazy, Suspense } from 'react';
import { CircularProgress } from '@mui/material';

const AccountListPage = lazy(() => import('../pages/AccountListPage'));
const TransferPage = lazy(() => import('../pages/TransferPage'));
const TransactionHistoryPage = lazy(() => import('../pages/TransactionHistoryPage'));

export const AppRoutes = () => (
  <Suspense fallback={<CircularProgress />}>
    <Routes>
      <Route path="/accounts" element={<AccountListPage />} />
      <Route path="/transfer" element={<TransferPage />} />
      <Route path="/transactions" element={<TransactionHistoryPage />} />
    </Routes>
  </Suspense>
);
```

2. **MUI Icons 按需載入:**
```typescript
// ❌ 錯誤:載入整個 icons 包
import { AccountBalance } from '@mui/icons-material';

// ✅ 正確:按需載入特定 icon
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
```

3. **Vite 手動分包配置:**
```typescript
// frontend/vite.config.ts
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'mui-core': ['@mui/material', '@mui/system'],
          'mui-icons': ['@mui/icons-material'],
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'otel': ['@opentelemetry/api', '@opentelemetry/sdk-trace-web'],
        },
      },
    },
  },
});
```

**效能目標 (Core Web Vitals):**
- **First Contentful Paint (FCP)**: < 1.5s
- **Largest Contentful Paint (LCP)**: < 2.5s
- **Time to Interactive (TTI)**: < 3.5s
- **Total Bundle Size**: < 500KB (gzipped)

**依賴審計流程:**
```json
// package.json scripts
{
  "scripts": {
    "audit": "npm audit",
    "audit:fix": "npm audit fix",
    "check-updates": "npx npm-check-updates",
    "analyze": "vite-bundle-visualizer"
  }
}
```

**自動化依賴更新:**
- 使用 Renovate Bot 自動檢查依賴更新
- MUI 套件群組更新(避免版本不一致)
- 每月執行 `npm audit` 安全檢查

**Affects:** React Frontend, UI/UX consistency, Bundle size optimization, Build performance

---

#### 4.4 Routing Strategy

**Decision: React Router v6**

**Routes Structure:**
```typescript
<Routes>
  <Route path="/" element={<HomePage />} />
  <Route path="/login" element={<LoginPage />} />
  <Route path="/accounts" element={<AccountListPage />} />
  <Route path="/accounts/:accountId" element={<AccountDetailPage />} />
  <Route path="/transfer" element={<TransferPage />} />
  <Route path="/transactions" element={<TransactionHistoryPage />} />
  <Route path="/exchange" element={<CurrencyExchangePage />} />
</Routes>
```

**Protected Routes:**
使用 `ProtectedRoute` wrapper 包裹需認證的頁面:
```typescript
<Route path="/accounts" element={
  <ProtectedRoute>
    <AccountListPage />
  </ProtectedRoute>
} />
```

**Affects:** React Frontend

---

#### 4.4 HTTP Client

**Decision: Axios**

**Rationale:**
- 易於配置 interceptors(自動注入 JWT token 與 trace context headers)
- 支援 request/response 攔截器(統一錯誤處理)
- TypeScript 型別支援佳

**Axios Configuration:**
```typescript
import axios from 'axios';
import { trace, context } from '@opentelemetry/api';

const apiClient = axios.create({
  baseURL: 'http://api-gateway:8080',
  timeout: 5000
});

// Request interceptor: 注入 JWT token 與 traceparent header
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // 注入 OpenTelemetry trace context
  const span = trace.getSpan(context.active());
  if (span) {
    const traceId = span.spanContext().traceId;
    const spanId = span.spanContext().spanId;
    config.headers['traceparent'] = `00-${traceId}-${spanId}-01`;
  }

  return config;
});

// Response interceptor: 統一錯誤處理
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token 過期,導向登入頁
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**Affects:** React Frontend

---

#### 4.5 OpenTelemetry Browser SDK

**Decision: Manual Instrumentation with Auto-Instrumentation Plugins**

**Dependencies:**
```json
{
  "dependencies": {
    "@opentelemetry/api": "^1.9.0",
    "@opentelemetry/sdk-trace-web": "^1.26.0",
    "@opentelemetry/instrumentation": "^0.53.0",
    "@opentelemetry/instrumentation-fetch": "^0.53.0",
    "@opentelemetry/instrumentation-xml-http-request": "^0.53.0",
    "@opentelemetry/exporter-trace-otlp-http": "^0.53.0"
  }
}
```

**Implementation:**
```typescript
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';

const provider = new WebTracerProvider({
  resource: new Resource({
    'service.name': 'frontend-app',
    'service.version': '1.0.0'
  })
});

provider.addSpanProcessor(
  new BatchSpanProcessor(
    new OTLPTraceExporter({
      url: 'http://otel-collector:4318/v1/traces'
    })
  )
);

provider.register();

// Auto-instrument fetch() calls
registerInstrumentations({
  instrumentations: [
    new FetchInstrumentation({
      propagateTraceHeaderCorsUrls: [/http:\/\/api-gateway:.*/]
    })
  ]
});
```

**Custom Spans for Business Operations:**
```typescript
import { trace } from '@opentelemetry/api';

const tracer = trace.getTracer('frontend-app');

function performTransfer(fromAccount: string, toAccount: string, amount: number) {
  const span = tracer.startSpan('ui.transfer.submit');
  span.setAttribute('account.from', fromAccount);
  span.setAttribute('account.to', toAccount);
  span.setAttribute('transaction.amount', amount);

  try {
    // API call
    await apiClient.post('/api/v1/transfers', { fromAccount, toAccount, amount });
    span.setStatus({ code: SpanStatusCode.OK });
  } catch (error) {
    span.setStatus({ code: SpanStatusCode.ERROR, message: error.message });
    span.recordException(error);
  } finally {
    span.end();
  }
}
```

**Affects:** React Frontend

---

### 5. Infrastructure & Deployment

#### 5.1 Deployment Tooling

**Decision: Helm Charts**

**Rationale:**
- 參數化配置(不同環境使用不同 `values.yaml`)
- 版本管理與回滾能力
- 社群生態系統豐富(可重用 Grafana, Prometheus, Kafka 官方 charts)

**Helm Chart Structure:**
```
helm/
├── lite-bank-demo/           # Umbrella chart
│   ├── Chart.yaml
│   ├── values.yaml          # 預設配置
│   ├── values-dev.yaml      # 開發環境
│   ├── values-staging.yaml  # Staging 環境
│   └── templates/
│       ├── namespace.yaml
│       ├── configmap.yaml
│       └── secret.yaml
├── charts/
│   ├── account-service/
│   ├── transfer-service/
│   ├── transaction-service/
│   └── ...
└── observability/           # Grafana Stack charts
    ├── grafana/
    ├── tempo/
    ├── loki/
    └── prometheus/
```

**One-Click Deployment Script:**
```bash
#!/bin/bash
# deploy.sh

# 1. 建立 Namespaces
kubectl apply -f k8s/namespaces.yaml

# 2. 部署基礎設施(PostgreSQL, Kafka, Chaos Mesh)
helm install postgres bitnami/postgresql -n banking-services -f helm/postgres-values.yaml
helm install kafka bitnami/kafka -n messaging -f helm/kafka-values.yaml
helm install chaos-mesh chaos-mesh/chaos-mesh -n chaos

# 3. 部署可觀測性工具鏈
helm install grafana grafana/grafana -n observability -f helm/observability/grafana/values.yaml
helm install tempo grafana/tempo -n observability -f helm/observability/tempo/values.yaml
helm install loki grafana/loki -n observability -f helm/observability/loki/values.yaml
helm install prometheus prometheus-community/prometheus -n observability

# 4. 部署微服務
helm install lite-bank-demo ./helm/lite-bank-demo -n banking-services

echo "✅ Deployment completed!"
```

**Affects:** All services and infrastructure components

---

#### 5.2 Ingress Controller

**Decision: Nginx Ingress (Optional Installation)**

**Installation (Optional):**
```bash
helm install nginx-ingress ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=LoadBalancer
```

**Ingress Configuration (if installed):**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: lite-bank-demo-ingress
  namespace: banking-services
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: lite-bank.local
    http:
      paths:
      - path: /api/v1/accounts
        pathType: Prefix
        backend:
          service:
            name: account-service
            port:
              number: 8080
      - path: /api/v1/transfers
        pathType: Prefix
        backend:
          service:
            name: transfer-service
            port:
              number: 8080
```

**Phase 1 Flexibility:**
- 可選擇不安裝 Ingress,直接透過 `kubectl port-forward` 存取服務
- Demo 展示時,Ingress 並非必要元件
- Phase 2 若需統一入口,再啟用 Ingress Controller

**Affects:** External access to microservices

---

#### 5.3 Configuration Management

**Decision: ConfigMap (Non-Sensitive) + Secret (Sensitive)**

**ConfigMap Usage:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: otel-collector-config
  namespace: observability
data:
  otel-collector-config.yaml: |
    receivers:
      otlp:
        protocols:
          http:
            endpoint: 0.0.0.0:4318
          grpc:
            endpoint: 0.0.0.0:4317
    processors:
      batch:
        timeout: 10s
    exporters:
      otlp/tempo:
        endpoint: tempo:4317
        tls:
          insecure: true
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch]
          exporters: [otlp/tempo]
```

**Secret Usage:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-credentials
  namespace: banking-services
type: Opaque
data:
  username: cG9zdGdyZXM=  # base64 encoded
  password: cGFzc3dvcmQ=  # base64 encoded
```

**Environment Variable Injection:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: account-service
spec:
  template:
    spec:
      containers:
      - name: account-service
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/banking"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: username
        - name: OTEL_EXPORTER_OTLP_ENDPOINT
          value: "http://otel-collector.observability:4318"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka.messaging:9092"
```

**Configuration Files:**
- 可觀測性配置:`otel-collector-config` ConfigMap
- 應用程式配置:各服務獨立 ConfigMap(如 `account-service-config`)
- 敏感資料:PostgreSQL/Kafka credentials 使用 Secret

**Affects:** All services

---

#### 5.4 Resource Limits & Requests

**Decision: Define Baseline Limits for Phase 1**

**Resource Configuration (Demo Scale):**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**Service-Specific Overrides:**
- **Transfer Service (SAGA Orchestrator)**:
  - requests: `cpu: 200m, memory: 512Mi`
  - limits: `cpu: 1000m, memory: 1Gi`
- **OTel Collector**:
  - requests: `cpu: 200m, memory: 512Mi`
  - limits: `cpu: 1000m, memory: 2Gi`(處理所有 traces)
- **Grafana/Tempo/Loki**: 使用官方 chart 預設值

**Monitoring:**
- Phase 1 不進行嚴格資源優化
- 透過 Prometheus 監控資源使用率,Phase 2 調整

**Affects:** All Kubernetes Deployments

---

### 6. Observability Implementation

#### 6.1 Log Format

**Decision: Structured JSON Logs**

**Log Format Structure:**
```json
{
  "timestamp": "2024-12-04T10:30:00.123Z",
  "level": "INFO",
  "service": "account-service",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "message": "Account balance updated successfully",
  "account_id": "TWD-001",
  "user_id": "user-001",
  "new_balance": 9500.00,
  "operation": "debit",
  "amount": 500.00
}
```

**Mandatory Fields:**
- `timestamp`: ISO 8601 格式
- `level`: ERROR, WARN, INFO, DEBUG
- `service`: 服務名稱(對應 OpenTelemetry `service.name`)
- `traceId`: OpenTelemetry trace ID(16 或 32 字元 hex)
- `spanId`: OpenTelemetry span ID(16 字元 hex)
- `message`: 人類可讀的日誌訊息

**Contextual Fields(視情況出現):**
- 業務欄位:`account_id`, `user_id`, `transaction_id`, `saga_id`, `amount` 等
- 錯誤欄位:`error_code`, `error_type`, `error_category`, `stack_trace`(ERROR level 時)
- 效能欄位:`duration_ms`, `http_status_code`, `http_method`, `http_path`

**Implementation Guidance:**

**Java (SpringBoot/Quarkus) - Logback/SLF4J:**
```xml
<!-- logback-spring.xml -->
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeContext>false</includeContext>
      <includeMdc>true</includeMdc>
      <fieldNames>
        <timestamp>timestamp</timestamp>
        <level>level</level>
        <logger>service</logger>
        <message>message</message>
      </fieldNames>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

**Java Code:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import io.opentelemetry.api.trace.Span;

Logger logger = LoggerFactory.getLogger(AccountService.class);

// 自動注入 traceId 與 spanId 至 MDC
Span currentSpan = Span.current();
MDC.put("traceId", currentSpan.getSpanContext().getTraceId());
MDC.put("spanId", currentSpan.getSpanContext().getSpanId());

// 業務日誌
MDC.put("account_id", "TWD-001");
MDC.put("operation", "debit");
MDC.put("amount", "500.00");
logger.info("Account balance updated successfully");
MDC.clear();
```

**Python (FastAPI) - structlog:**
```python
import structlog
from opentelemetry import trace

logger = structlog.get_logger()

span = trace.get_current_span()
logger.info(
    "Account balance updated successfully",
    traceId=format(span.get_span_context().trace_id, '032x'),
    spanId=format(span.get_span_context().span_id, '016x'),
    account_id="TWD-001",
    operation="debit",
    amount=500.00
)
```

**Log Aggregation:**
- 所有服務日誌輸出至 `stdout`
- Kubernetes 自動收集並轉發至 Loki
- Grafana Loki 提供 `{traceId}` 查詢功能,實現 log ↔ trace 跳轉

**Affects:** All microservices

---

#### 6.2 Metrics Scope

**Decision: Application-Level Metrics Only**

**Included Metrics:**

1. **HTTP Metrics:**
   - `http_server_requests_total{method, endpoint, status}` - 請求總數
   - `http_server_request_duration_seconds{method, endpoint}` - 請求延遲(histogram)
   - `http_server_active_requests{method, endpoint}` - 當前活躍請求數

2. **JVM Metrics (Java Services):**
   - `jvm_memory_used_bytes{area}` - JVM 記憶體使用量
   - `jvm_gc_pause_seconds` - GC 暫停時間
   - `jvm_threads_current` - 執行緒數量

3. **Database Connection Pool Metrics:**
   - `hikaricp_connections_active` - 活躍連線數
   - `hikaricp_connections_idle` - 閒置連線數
   - `hikaricp_connections_pending` - 等待中連線數

4. **Kafka Metrics:**
   - `kafka_producer_record_send_total` - 發送訊息總數
   - `kafka_consumer_records_consumed_total` - 消費訊息總數

5. **OpenTelemetry Metrics:**
   - `otelcol_receiver_accepted_spans` - OTel Collector 接收的 spans 數量
   - `otelcol_exporter_sent_spans` - OTel Collector 發送的 spans 數量

**Excluded Metrics (Not Implemented in Phase 1):**
- ❌ 業務 metrics:`banking_account_balance`, `banking_transaction_count_by_currency`
- **Rationale**: Metrics 用於監控系統健康度,非業務資料展示;使用者餘額應在 UI 顯示,非 Grafana dashboard

**Metrics Exposition:**
- Java Services: `/actuator/prometheus` endpoint(Spring Boot Actuator + Micrometer)
- Python Services: `/metrics` endpoint(Prometheus Python client)
- Frontend: 不暴露 metrics endpoint(OTel Browser SDK 僅發送 traces)

**Prometheus Scrape Configuration:**
```yaml
scrape_configs:
  - job_name: 'account-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ['banking-services']
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: account-service
      - source_labels: [__meta_kubernetes_pod_ip]
        target_label: __address__
        replacement: ${1}:8080
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: instance
```

**Affects:** All microservices, Prometheus, Grafana dashboards

---

#### 6.3 OpenTelemetry Collector Sampling Strategy

**Decision: Flexible Configuration (Default Full Sampling)**

**Sampling Strategy Overview:**

| Strategy | Use Case | Trace Retention | Data Volume (500 req/min) |
|----------|----------|-----------------|---------------------------|
| **Full Sampling (100%)** | Demo 展示、開發環境 | 所有 traces | ~500 KB/min |
| **Probabilistic Sampling (10%)** | 壓測場景(高流量) | 10% traces | ~50 KB/min |
| **Tail Sampling** | 生產環境(錯誤優先) | 錯誤 100%, SAGA 50%, 正常 5% | ~100 KB/min |

**Default Configuration: Full Sampling**

`otel-collector-config.yaml` (預設配置):
```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318
      grpc:
        endpoint: 0.0.0.0:4317

processors:
  batch:
    timeout: 10s
    send_batch_size: 100

exporters:
  otlp/tempo:
    endpoint: tempo.observability:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo]
```

**Alternative Configuration 1: Probabilistic Sampling**

`otel-collector-config-probabilistic.yaml` (壓測場景):
```yaml
processors:
  batch:
    timeout: 10s
  probabilistic_sampler:
    sampling_percentage: 10  # 僅保留 10% traces

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, probabilistic_sampler]
      exporters: [otlp/tempo]
```

**Alternative Configuration 2: Tail Sampling**

`otel-collector-config-tail-sampling.yaml` (智能採樣):
```yaml
processors:
  batch:
    timeout: 10s
  tail_sampling:
    decision_wait: 10s
    num_traces: 1000
    expected_new_traces_per_sec: 10
    policies:
      # Policy 1: 保留所有錯誤 traces
      - name: error-traces
        type: status_code
        status_code:
          status_codes: [ERROR]

      # Policy 2: 保留所有 SAGA 相關 traces (50% 採樣)
      - name: saga-traces
        type: and
        and:
          and_sub_policy:
            - name: has-saga-attribute
              type: attribute
              attribute:
                key: saga.id
                values: [".*"]
            - name: probabilistic-50
              type: probabilistic
              probabilistic:
                sampling_percentage: 50

      # Policy 3: 正常 traces 僅保留 5%
      - name: normal-traces
        type: probabilistic
        probabilistic:
          sampling_percentage: 5

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [tail_sampling, batch]
      exporters: [otlp/tempo]
```

**Configuration Switching:**

**Method 1: ConfigMap 切換**
```bash
# 使用預設 Full Sampling
kubectl apply -f k8s/configmaps/otel-collector-config.yaml

# 切換至 Tail Sampling(壓測場景)
kubectl apply -f k8s/configmaps/otel-collector-config-tail-sampling.yaml
kubectl rollout restart deployment/otel-collector -n observability
```

**Method 2: Helm Values 切換**
```yaml
# helm/observability/otel-collector/values.yaml
config:
  samplingStrategy: "full"  # Options: full, probabilistic, tail

# 部署時指定
helm install otel-collector ./helm/observability/otel-collector \
  --set config.samplingStrategy=tail
```

**Data Volume Analysis:**

**Scenario: 500 requests/min, Average trace size 1KB**

| Sampling Strategy | Traces Stored/min | Data Volume/min | Daily Volume |
|-------------------|-------------------|-----------------|--------------|
| Full (100%) | 500 | 500 KB | ~720 MB |
| Probabilistic (10%) | 50 | 50 KB | ~72 MB |
| Tail Sampling | ~100 (errors 100%, SAGA 50%, normal 5%) | ~100 KB | ~144 MB |

**Stress Test Scenario: 10,000 requests/min**

| Sampling Strategy | Data Volume/min | Daily Volume |
|-------------------|-----------------|--------------|
| Full (100%) | 10 MB | ~14.4 GB ⚠️ |
| Probabilistic (10%) | 1 MB | ~1.44 GB ✅ |
| Tail Sampling | 2 MB | ~2.88 GB ✅ |

**Recommendation:**
- **Demo 展示**: Full Sampling(完整追蹤體驗)
- **壓測場景**: Probabilistic (10%) 或 Tail Sampling(智能採樣)
- **切換時機**: 透過 ConfigMap 或 Helm values 即時切換,無需修改服務程式碼

**Affects:** OTel Collector, Tempo, All microservices

---

### 7. Decision Impact Analysis

#### Implementation Sequence

建議的實作順序(依賴關係排列):

**Phase 1: Infrastructure Foundation**
1. K8s Namespaces 與基礎網路設定
2. PostgreSQL 部署與 Flyway schema 初始化
3. Kafka 部署與 topic 建立
4. Grafana Stack 部署(Tempo, Loki, Prometheus, Grafana)
5. OTel Collector 部署(預設 Full Sampling)

**Phase 2: Core Microservices**
6. User Service(認證服務,其他服務依賴 JWT 驗證)
7. Account Service(帳戶管理,Transfer Service 依賴)
8. Currency Exchange Service(匯率查詢,Transfer Service 依賴)
9. Transaction Service(交易記錄,Transfer Service 依賴)
10. Transfer Service(SAGA 編排器,整合上述服務)
11. Notification Service(Kafka 消費者 + WebSocket 推播)

**Phase 3: Frontend & Observability Integration**
12. React Frontend(整合 OTel Browser SDK)
13. OpenTelemetry Conventions 文件(`docs/opentelemetry-conventions.md`)
14. Error Code Mapping 文件(`docs/error-codes.md`)
15. Database Access Rules 文件(`docs/database-access-rules.md`)
16. Exception Hierarchy 文件(`docs/exception-hierarchy.md`)

**Phase 4: Chaos Engineering**
17. Chaos Mesh 部署
18. Chaos 場景定義與測試(`chaos-scenarios/`)

#### Cross-Component Dependencies

**Decision Dependencies Matrix:**

| Decision | Depends On | Affects |
|----------|-----------|---------|
| Flyway Migration | PostgreSQL 部署 | Account/Transaction/User Services schema |
| Shared Schema Strategy | Database Access Rules 文件 | 所有微服務的資料存取邏輯 |
| Unified Error Format | Error Code Mapping 文件 | 所有 API error responses |
| OTel Collector Sampling | Tempo 部署 | 所有服務的 trace exporter 配置 |
| JWT Authentication | User Service 實作 | 所有需認證的 API endpoints |
| WebSocket Trace Context | Socket.io-client 整合 | Notification Service + Frontend |
| Structured JSON Logs | Loki 部署 | 所有服務的 logging 配置 |

**Critical Path:**
PostgreSQL → Flyway → User Service → Account Service → Transfer Service → Frontend

**Parallel Implementation Opportunities:**
- Currency Exchange Service 與 Transaction Service 可並行開發(與 Account Service 無依賴)
- Grafana Stack 可與微服務開發並行部署
- Chaos Mesh 可在系統穩定後獨立整合

---

### 8. Supporting Documentation Requirements

為確保架構決策能正確實作,需建立以下支援文件:

#### 8.1 `docs/database-access-rules.md`

**Required Content:**
- 服務-表格存取權限矩陣(Read/Write 權限明確定義)
- Code Review 檢查清單(禁止跨服務直接寫入檢查)
- 資料庫連接池配置指引
- 違規案例與正確實作對照

**Owner:** Architect + Database Administrator

---

#### 8.2 `docs/error-codes.md`

**Required Content:**
- 完整錯誤碼對照表(ERR_001 ~ ERR_0XX)
- 錯誤分類說明(business, external_dependency, system)
- HTTP 狀態碼對應關係
- 錯誤回應 JSON 範例
- 新增錯誤碼的流程(避免重複編碼)

**Owner:** API Team Lead

---

#### 8.3 `docs/opentelemetry-conventions.md`

**Required Content:**
- Span attributes 命名規範(帳戶、交易、SAGA、錯誤等)
- Trace context propagation 實作指引(HTTP headers, Kafka headers, WebSocket)
- 各語言常量類別定義(Java, Python, TypeScript)
- Span 建立與標註最佳實踐
- 錯誤 span 標註方式

**Owner:** Observability Team Lead

---

#### 8.4 `docs/exception-hierarchy.md`

**Required Content:**
- 自訂 Exception 類別階層圖
- 各類別的錯誤碼對應
- Exception 建立與拋出範例
- Exception 與 OpenTelemetry span 整合方式

**Owner:** Backend Team Lead

---

## Next Steps

所有核心架構決策已完成,下一步將進入 **Step 5: Implementation Patterns**(實作模式定義),確保 AI agents 在實作時遵循統一的編碼規範與設計模式。

**Pending Actions:**
1. 建立支援文件(`database-access-rules.md`, `error-codes.md`, `opentelemetry-conventions.md`, `exception-hierarchy.md`)
2. 準備多套 OTel Collector 配置檔案(Full Sampling, Probabilistic, Tail Sampling)
3. 進入 Step 5: 定義實作模式(coding conventions, testing strategies, CI/CD patterns)

---

## Implementation Patterns & Consistency Rules

_本章節定義實作模式與一致性規則,確保多個 AI agents 撰寫的程式碼能夠無縫協作,避免命名、結構、格式等衝突。_

### Pattern Categories Overview

**潛在衝突點已識別:** 基於 Java (SpringBoot, Quarkus)、Python (FastAPI)、React (TypeScript) 三種語言技術棧,識別出 **5 大類 25+ 個潛在衝突點**,需要明確的一致性規則。

---

### 1. Naming Patterns (命名模式)

#### 1.1 Database Naming Conventions

**Table Naming:**
- **規則**: 小寫 snake_case,複數形式
- **範例**:
  - ✅ `accounts`, `users`, `transactions`, `saga_executions`
  - ❌ `Account`, `user`, `Transaction`

**Column Naming:**
- **規則**: 小寫 snake_case
- **範例**:
  - ✅ `user_id`, `account_id`, `created_at`, `transaction_amount`
  - ❌ `userId`, `AccountID`, `createdAt`

**Primary Key Naming:**
- **規則**: `{table_singular}_id` 或業務主鍵
- **範例**:
  - ✅ `user_id`, `account_id`, `saga_id` (UUID)
  - ✅ `account_id` (業務主鍵,格式:`TWD-001`)

**Foreign Key Naming:**
- **規則**: 與參照表的主鍵同名(不加 `fk_` 前綴)
- **範例**:
  - ✅ `user_id` (參照 `users.user_id`)
  - ❌ `fk_user_id`, `userId`

**Index Naming:**
- **規則**: `idx_{table}_{column1}_{column2}`
- **範例**:
  - ✅ `idx_accounts_user_id`, `idx_saga_executions_status`
  - ❌ `accounts_user_id_index`, `user_idx`

**Constraint Naming:**
- **規則**: `{type}_{table}_{detail}`
- **範例**:
  - ✅ `fk_user` (Foreign Key), `chk_balance` (Check Constraint), `unq_email` (Unique)
  - ❌ `user_fk`, `balance_check`

---

#### 1.2 API Naming Conventions

**REST Endpoint Naming:**
- **規則**: 小寫,複數資源名稱,kebab-case 用於複合詞
- **格式**: `/api/v{version}/{resource}`
- **範例**:
  - ✅ `GET /api/v1/accounts`, `POST /api/v1/transfers`, `GET /api/v1/exchange-rates`
  - ❌ `/api/v1/account`, `/api/v1/Transfer`, `/api/v1/exchangeRates`

**Path Parameter Naming:**
- **規則**: camelCase,使用 `{paramName}` 格式
- **範例**:
  - ✅ `GET /api/v1/accounts/{accountId}`
  - ✅ `GET /api/v1/transactions/{transactionId}`
  - ❌ `/api/v1/accounts/:account_id`, `/api/v1/accounts/{id}`

**Query Parameter Naming:**
- **規則**: camelCase
- **範例**:
  - ✅ `GET /api/v1/transactions?accountId=TWD-001&startDate=2024-12-01`
  - ❌ `?account_id=`, `?start_date=`

**HTTP Header Naming:**
- **規則**: 標準 headers 使用規範名稱,自訂 headers 使用 `X-` 前綴(camelCase 或 kebab-case)
- **範例**:
  - ✅ `Authorization`, `Content-Type`, `traceparent` (W3C 標準)
  - ✅ `X-Request-Id`, `X-Trace-Id` (自訂)
  - ❌ `authorization`, `CONTENT-TYPE`

---

#### 1.3 Code Naming Conventions

**Java (SpringBoot / Quarkus):**

**Class Naming:**
- **規則**: PascalCase,名詞或名詞片語
- **範例**:
  - ✅ `AccountService`, `TransferController`, `SagaExecutionRepository`
  - ❌ `accountService`, `Transfer_Controller`, `sagaRepo`

**Method Naming:**
- **規則**: camelCase,動詞開頭
- **範例**:
  - ✅ `getAccountById()`, `createTransfer()`, `validateBalance()`
  - ❌ `GetAccount()`, `account_by_id()`, `validate_balance()`

**Variable Naming:**
- **規則**: camelCase
- **範例**:
  - ✅ `accountId`, `transactionAmount`, `userId`
  - ❌ `account_id`, `TransactionAmount`, `user_ID`

**Constant Naming:**
- **規則**: UPPER_SNAKE_CASE
- **範例**:
  - ✅ `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS`, `SAGA_STEP_FREEZE_ACCOUNT`
  - ❌ `maxRetryAttempts`, `default_timeout`, `SagaStepFreezeAccount`

**Package Naming:**
- **規則**: 全小寫,點號分隔,反向域名
- **範例**:
  - ✅ `com.bank.account.service`, `com.bank.transfer.saga`
  - ❌ `com.bank.Account.Service`, `com.bank.transfer_saga`

**Python (FastAPI):**

**Module/File Naming:**
- **規則**: 小寫 snake_case
- **範例**:
  - ✅ `account_service.py`, `saga_orchestrator.py`, `otel_config.py`
  - ❌ `AccountService.py`, `sagaOrchestrator.py`

**Function Naming:**
- **規則**: 小寫 snake_case,動詞開頭
- **範例**:
  - ✅ `get_account_by_id()`, `create_transfer()`, `validate_balance()`
  - ❌ `getAccountById()`, `CreateTransfer()`

**Class Naming:**
- **規則**: PascalCase
- **範例**:
  - ✅ `TransferRequest`, `SagaOrchestrator`, `AccountResponse`
  - ❌ `transfer_request`, `sagaOrchestrator`

**Variable Naming:**
- **規則**: 小寫 snake_case
- **範例**:
  - ✅ `account_id`, `transaction_amount`, `user_id`
  - ❌ `accountId`, `TransactionAmount`

**Constant Naming:**
- **規則**: UPPER_SNAKE_CASE
- **範例**:
  - ✅ `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_SECONDS`

**TypeScript / React:**

**Component File Naming:**
- **規則**: PascalCase (`.tsx` 檔案)
- **範例**:
  - ✅ `AccountCard.tsx`, `TransferForm.tsx`, `LoginPage.tsx`
  - ❌ `account-card.tsx`, `transferForm.tsx`, `login_page.tsx`

**Component Naming:**
- **規則**: PascalCase,與檔案名稱一致
- **範例**:
  - ✅ `export const AccountCard = () => {...}`
  - ❌ `export const accountCard = () => {...}`

**Hook Naming:**
- **規則**: camelCase,`use` 前綴
- **範例**:
  - ✅ `useAuth()`, `useAccounts()`, `useWebSocket()`
  - ❌ `UseAuth()`, `use_accounts()`

**Utility File Naming:**
- **規則**: camelCase (`.ts` 檔案)
- **範例**:
  - ✅ `apiClient.ts`, `formatCurrency.ts`, `otelConfig.ts`
  - ❌ `ApiClient.ts`, `format_currency.ts`

**Variable/Function Naming:**
- **規則**: camelCase
- **範例**:
  - ✅ `accountId`, `fetchAccounts()`, `handleSubmit()`
  - ❌ `account_id`, `FetchAccounts()`

**Constant Naming:**
- **規則**: UPPER_SNAKE_CASE
- **範例**:
  - ✅ `API_BASE_URL`, `MAX_RETRIES`

---

### 2. Structure Patterns (結構模式)

#### 2.1 Java Project Structure

**SpringBoot Service Structure:**
```
account-service/
├── src/
│   ├── main/
│   │   ├── java/com/bank/account/
│   │   │   ├── controller/
│   │   │   │   └── AccountController.java
│   │   │   ├── service/
│   │   │   │   ├── AccountService.java
│   │   │   │   └── AccountServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   └── AccountRepository.java
│   │   │   ├── model/
│   │   │   │   ├── Account.java
│   │   │   │   └── AccountRequest.java
│   │   │   ├── exception/
│   │   │   │   ├── AccountNotFoundException.java
│   │   │   │   └── InsufficientBalanceException.java
│   │   │   ├── config/
│   │   │   │   ├── OpenTelemetryConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   └── AccountServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── db/migration/
│   │           └── V1__create_accounts_table.sql
│   └── test/
│       └── java/com/bank/account/
│           ├── controller/
│           │   └── AccountControllerTest.java
│           ├── service/
│           │   └── AccountServiceTest.java
│           └── repository/
│               └── AccountRepositoryTest.java
├── pom.xml (or build.gradle)
└── README.md
```

**Test Location:** ✅ **Separate Directory** (`src/test/java/`)

**Package Organization:**
- By layer (controller, service, repository, model)
- Exception package 獨立
- Config package 集中配置類別

---

#### 2.2 Python Project Structure

**FastAPI Service Structure:**
```
transfer-service/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── __init__.py
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   └── transfer.py
│   │   └── dependencies.py
│   ├── services/
│   │   ├── __init__.py
│   │   ├── saga_orchestrator.py
│   │   └── account_client.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── transfer_request.py
│   │   └── saga_execution.py
│   ├── exceptions/
│   │   ├── __init__.py
│   │   └── transfer_exceptions.py
│   ├── config/
│   │   ├── __init__.py
│   │   ├── settings.py
│   │   └── otel_config.py
│   └── utils/
│       ├── __init__.py
│       └── otel_attributes.py
├── tests/
│   ├── __init__.py
│   ├── test_saga_orchestrator.py
│   └── test_transfer_api.py
├── requirements.txt
├── Dockerfile
└── README.md
```

**Test Location:** ✅ **Separate Directory** (`tests/`)

**Module Organization:**
- `api/`: FastAPI routes & dependencies
- `services/`: Business logic
- `models/`: Pydantic models
- `config/`: Settings & OTel config

---

#### 2.3 React Project Structure

**Frontend Structure:**
```
frontend/
├── public/
│   ├── index.html
│   └── favicon.ico
├── src/
│   ├── index.tsx
│   ├── App.tsx
│   ├── components/
│   │   ├── common/
│   │   │   ├── ErrorBoundary.tsx
│   │   │   └── LoadingSpinner.tsx
│   │   ├── accounts/
│   │   │   ├── AccountCard.tsx
│   │   │   └── AccountList.tsx
│   │   └── transfers/
│   │       └── TransferForm.tsx
│   ├── pages/
│   │   ├── HomePage.tsx
│   │   ├── LoginPage.tsx
│   │   ├── AccountListPage.tsx
│   │   └── TransferPage.tsx
│   ├── contexts/
│   │   ├── AuthContext.tsx
│   │   ├── AccountContext.tsx
│   │   └── NotificationContext.tsx
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useAccounts.ts
│   │   └── useWebSocket.ts
│   ├── services/
│   │   ├── apiClient.ts
│   │   ├── accountService.ts
│   │   └── transferService.ts
│   ├── utils/
│   │   ├── formatCurrency.ts
│   │   ├── otelConfig.ts
│   │   └── constants.ts
│   ├── types/
│   │   ├── account.ts
│   │   ├── transfer.ts
│   │   └── api.ts
│   └── styles/
│       └── theme.ts
├── package.json
├── tsconfig.json
└── README.md
```

**Component Organization:** By feature + common
**Test Location:** Co-located (`AccountCard.test.tsx` 與 `AccountCard.tsx` 同目錄) 或 `src/__tests__/`

---

### 3. Format Patterns (格式模式)

#### 3.1 JSON Field Naming Convention

**統一規則: camelCase (跨所有語言)**

**API Request/Response 範例:**
```json
{
  "accountId": "TWD-001",
  "userId": "user-001",
  "balance": 10000.00,
  "currency": "TWD",
  "createdAt": "2024-12-04T10:30:00Z",
  "transactionAmount": 1000.00
}
```

**語言特定實作:**

**Java (SpringBoot/Quarkus):**
- 使用 Jackson 預設 camelCase 序列化
- 無需額外配置

**Python (FastAPI):**
- 使用 Pydantic `alias` 將 snake_case 轉換為 camelCase

**範例:**
```python
from pydantic import BaseModel, Field

class AccountResponse(BaseModel):
    account_id: str = Field(..., alias="accountId")
    user_id: str = Field(..., alias="userId")
    balance: float
    currency: str
    created_at: datetime = Field(..., alias="createdAt")

    class Config:
        populate_by_name = True  # 允許兩種命名接受
        json_schema_extra = {
            "example": {
                "accountId": "TWD-001",
                "userId": "user-001",
                "balance": 10000.00,
                "currency": "TWD",
                "createdAt": "2024-12-04T10:30:00Z"
            }
        }
```

**TypeScript (React):**
- Interface 使用 camelCase(與 JSON 一致)

**範例:**
```typescript
interface Account {
  accountId: string;
  userId: string;
  balance: number;
  currency: string;
  createdAt: string;
}
```

---

#### 3.2 Date/Time Format

**API 傳輸格式:**
- **規則**: ISO 8601 字串,UTC 時區
- **格式**: `YYYY-MM-DDTHH:mm:ss.sssZ`
- **範例**:
  - ✅ `"2024-12-04T10:30:00.123Z"`
  - ❌ `1733312400000` (Unix timestamp), `"2024-12-04 10:30:00"`

**Java 實作:**
```java
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class Transaction {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;
}
```

**Python 實作:**
```python
from datetime import datetime
from pydantic import BaseModel, Field

class Transaction(BaseModel):
    created_at: datetime = Field(..., alias="createdAt")

    class Config:
        json_encoders = {
            datetime: lambda v: v.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
        }
```

**TypeScript 實作:**
```typescript
// 儲存為 ISO string
interface Transaction {
  createdAt: string;  // ISO 8601 format
}

// 顯示時轉換
const displayDate = new Date(transaction.createdAt).toLocaleString('zh-TW');
```

---

#### 3.3 API Success Response Format

**統一規則: 直接回傳 data(不包裝)**

**Single Resource:**
```json
{
  "accountId": "TWD-001",
  "userId": "user-001",
  "balance": 10000.00,
  "currency": "TWD"
}
```

**Collection:**
```json
[
  {
    "accountId": "TWD-001",
    "userId": "user-001",
    "balance": 10000.00
  },
  {
    "accountId": "USD-001",
    "userId": "user-001",
    "balance": 500.00
  }
]
```

**❌ 不使用包裝格式:**
```json
{
  "data": { ... },
  "meta": { ... }
}
```

**例外:分頁回應需要 metadata**
```json
{
  "items": [...],
  "totalCount": 100,
  "page": 1,
  "pageSize": 20
}
```

---

#### 3.4 API Error Response Format

**已在 Step 4 定義,此處重申一致性:**

```json
{
  "error": {
    "code": "ERR_001",
    "type": "INSUFFICIENT_BALANCE",
    "message": "餘額不足,目前餘額 500 元,需要 1000 元",
    "category": "business",
    "details": {
      "accountId": "TWD-001",
      "currentBalance": 500,
      "requiredAmount": 1000
    },
    "traceId": "abc123def456",
    "timestamp": "2024-12-04T10:30:00Z"
  }
}
```

**注意:** `details` 欄位內的 key 也使用 camelCase

---

### 4. Communication Patterns (通訊模式)

#### 4.1 Kafka Event Naming

**Event Type Naming:**
- **規則**: UPPER_SNAKE_CASE,{RESOURCE}_{ACTION} 格式
- **範例**:
  - ✅ `TRANSFER_SUCCESS`, `TRANSFER_FAILED`, `ACCOUNT_CREATED`, `SAGA_COMPENSATED`
  - ❌ `transfer.success`, `TransferSuccess`, `transfer-success`

**Kafka Message Payload:**
```json
{
  "notificationId": "notif-001",
  "userId": "user-001",
  "type": "TRANSFER_SUCCESS",
  "message": "轉帳成功!已從帳戶 TWD-001 轉出 1000 元",
  "_trace": {
    "traceId": "abc123",
    "spanId": "def456"
  },
  "timestamp": "2024-12-04T10:30:00Z"
}
```

**注意:** Payload 欄位使用 camelCase

---

#### 4.2 WebSocket Event Naming

**Event Name:**
- **規則**: kebab-case 或 camelCase
- **範例**:
  - ✅ `notification`, `account-updated`, `transfer-completed`
  - ❌ `NOTIFICATION`, `account_updated`

**WebSocket Message Format:**
```json
{
  "messageId": "msg-001",
  "type": "notification",
  "payload": {
    "notificationId": "notif-001",
    "message": "轉帳成功!",
    "accountId": "TWD-001"
  },
  "_trace": {
    "traceId": "abc123",
    "spanId": "def456"
  }
}
```

---

#### 4.3 React State Management Patterns

**Context API Updates:**
- **規則**: Immutable updates,使用 spread operator
- **範例**:

**✅ 正確:**
```typescript
const addAccount = (newAccount: Account) => {
  setAccounts(prevAccounts => [...prevAccounts, newAccount]);
};

const updateAccount = (accountId: string, updatedData: Partial<Account>) => {
  setAccounts(prevAccounts =>
    prevAccounts.map(acc =>
      acc.accountId === accountId ? { ...acc, ...updatedData } : acc
    )
  );
};
```

**❌ 錯誤:**
```typescript
const addAccount = (newAccount: Account) => {
  accounts.push(newAccount);  // ❌ Direct mutation
  setAccounts(accounts);
};
```

---

### 5. Process Patterns (流程模式)

#### 5.1 Loading State Naming

**State Variable Naming:**
- **規則**: `isLoading` (boolean) 或 `loadingState` (enum)
- **範例**:

**Boolean Loading:**
```typescript
const [isLoading, setIsLoading] = useState(false);
const [isSubmitting, setIsSubmitting] = useState(false);
```

**Enum Loading (複雜場景):**
```typescript
type LoadingState = 'idle' | 'loading' | 'success' | 'error';
const [loadingState, setLoadingState] = useState<LoadingState>('idle');
```

---

#### 5.2 Error Handling Patterns

**Frontend Error Boundary:**
```typescript
// ErrorBoundary.tsx (使用 react-error-boundary 或自訂)
import { ErrorBoundary as ReactErrorBoundary } from 'react-error-boundary';

function ErrorFallback({ error }: { error: Error }) {
  return (
    <div>
      <h2>發生錯誤</h2>
      <p>{error.message}</p>
    </div>
  );
}

// 使用
<ReactErrorBoundary FallbackComponent={ErrorFallback}>
  <App />
</ReactErrorBoundary>
```

**Backend Exception Handler:**

**Java (SpringBoot):**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        ErrorResponse error = new ErrorResponse(
            "ERR_001",
            "INSUFFICIENT_BALANCE",
            ex.getMessage(),
            "business",
            Map.of("accountId", ex.getAccountId(), "currentBalance", ex.getCurrentBalance()),
            Span.current().getSpanContext().getTraceId(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

**Python (FastAPI):**
```python
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI()

@app.exception_handler(InsufficientBalanceException)
async def insufficient_balance_handler(request: Request, exc: InsufficientBalanceException):
    span = trace.get_current_span()
    return JSONResponse(
        status_code=400,
        content={
            "error": {
                "code": "ERR_001",
                "type": "INSUFFICIENT_BALANCE",
                "message": str(exc),
                "category": "business",
                "details": {"accountId": exc.account_id},
                "traceId": format(span.get_span_context().trace_id, '032x'),
                "timestamp": datetime.utcnow().isoformat() + 'Z'
            }
        }
    )
```

---

### 6. Testing Patterns (測試模式)

#### 6.1 Test File Organization

**Java:**
- ✅ **Separate Directory**: `src/test/java/`
- Test class naming: `{ClassName}Test.java`
- 範例:`AccountServiceTest.java`, `TransferControllerTest.java`

**Python:**
- ✅ **Separate Directory**: `tests/`
- Test file naming: `test_{module_name}.py`
- 範例:`test_saga_orchestrator.py`, `test_transfer_api.py`

**TypeScript/React:**
- ✅ **Co-located** 或 **Separate**: `src/__tests__/` 或與元件同目錄
- Test file naming: `{ComponentName}.test.tsx`
- 範例:`AccountCard.test.tsx`, `TransferForm.test.tsx`

---

#### 6.2 Test Naming Conventions

**Java (JUnit):**
```java
@Test
void shouldReturnAccountWhenAccountExists() { ... }

@Test
void shouldThrowInsufficientBalanceExceptionWhenBalanceIsLow() { ... }
```

**Python (pytest):**
```python
def test_create_transfer_success(): ...

def test_saga_orchestrator_compensates_on_failure(): ...
```

**TypeScript (Jest/React Testing Library):**
```typescript
describe('AccountCard', () => {
  it('should display account balance correctly', () => { ... });

  it('should show loading state when fetching data', () => { ... });
});
```

---

### 7. Enforcement Guidelines (執行指引)

#### All AI Agents MUST:

1. **遵循 JSON camelCase 規範**
   - Python agents 必須使用 Pydantic `alias` 轉換
   - 所有 API request/response 欄位使用 camelCase

2. **遵循測試檔案分離規範**
   - Java/Python: 測試檔案放置於 `src/test/` 或 `tests/`
   - React: 可 co-located 或 `src/__tests__/`

3. **遵循檔案命名規範**
   - Java class: PascalCase
   - Python module: snake_case
   - React component: PascalCase.tsx

4. **遵循錯誤回應格式**
   - 使用統一的 JSON error structure (ERR_001 格式)
   - 包含 `traceId` 欄位

5. **遵循 OpenTelemetry 規範**
   - 參考 `docs/opentelemetry-conventions.md`
   - Span attributes 使用統一命名(如 `account.id`, `transaction.amount`)

#### Pattern Verification:

**Code Review Checklist:**
- [ ] JSON 欄位是否使用 camelCase?
- [ ] 測試檔案是否放置於正確位置?
- [ ] 檔案命名是否符合語言慣例?
- [ ] 錯誤回應是否包含 `code`, `type`, `category`, `traceId`?
- [ ] 日期格式是否使用 ISO 8601?
- [ ] Database 命名是否使用 snake_case?
- [ ] API endpoint 是否使用複數資源名稱?

**Pattern Violation Handling:**
- 發現違規時,記錄於 Code Review comments
- 要求修正後再 merge

**Pattern Updates:**
- 若需新增或修改模式,更新 `docs/architecture.md` 此章節
- 通知所有開發者(包含 AI agents)

---

### 8. Pattern Examples (模式範例)

#### Good Examples (正確範例)

**1. Java Service with Proper Naming:**
```java
package com.bank.account.controller;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        Account account = accountService.findById(accountId);
        return ResponseEntity.ok(new AccountResponse(
            account.getAccountId(),
            account.getUserId(),
            account.getBalance(),
            account.getCurrency()
        ));
    }
}

// Response JSON:
// {
//   "accountId": "TWD-001",
//   "userId": "user-001",
//   "balance": 10000.00,
//   "currency": "TWD"
// }
```

**2. Python FastAPI with Pydantic Aliases:**
```python
from pydantic import BaseModel, Field

class TransferRequest(BaseModel):
    from_account_id: str = Field(..., alias="fromAccountId")
    to_account_id: str = Field(..., alias="toAccountId")
    amount: float
    currency: str

@app.post("/api/v1/transfers")
async def create_transfer(request: TransferRequest):
    # Business logic
    return {
        "transferId": transfer_id,
        "fromAccountId": request.from_account_id,
        "toAccountId": request.to_account_id,
        "amount": request.amount,
        "status": "COMPLETED"
    }
```

**3. React Component with Proper Structure:**
```typescript
// src/components/accounts/AccountCard.tsx
import React from 'react';
import { Account } from '../../types/account';

interface AccountCardProps {
  account: Account;
  onSelect: (accountId: string) => void;
}

export const AccountCard: React.FC<AccountCardProps> = ({ account, onSelect }) => {
  const handleClick = () => {
    onSelect(account.accountId);
  };

  return (
    <div onClick={handleClick}>
      <h3>{account.currency} Account</h3>
      <p>Balance: {account.balance.toFixed(2)}</p>
    </div>
  );
};
```

---

#### Anti-Patterns (應避免的錯誤)

**❌ 1. 混用 snake_case 與 camelCase:**
```json
{
  "account_id": "TWD-001",     // ❌ snake_case
  "userId": "user-001",        // ✅ camelCase
  "Balance": 10000.00,         // ❌ PascalCase
  "currency": "TWD"
}
```

**❌ 2. 直接 mutation state (React):**
```typescript
const addAccount = (newAccount: Account) => {
  accounts.push(newAccount);  // ❌ Direct mutation
  setAccounts(accounts);
};
```

**❌ 3. 不一致的錯誤回應:**
```json
{
  "errorCode": "001",          // ❌ 應為 "ERR_001"
  "msg": "餘額不足",           // ❌ 應為 "message"
  "details": null              // ❌ 應為 object 或省略
}
```

**❌ 4. 不正確的測試位置:**
```
account-service/
├── src/main/java/com/bank/account/
│   ├── service/AccountService.java
│   └── service/AccountServiceTest.java    // ❌ Test 與 main code 混合
```

**❌ 5. 不一致的 endpoint 命名:**
```
GET /api/v1/account          // ❌ 單數
GET /api/v1/Transfer         // ❌ PascalCase
GET /api/v1/exchange_rates   // ❌ snake_case
```

---

## Pattern Implementation Priority

**Critical (Must Implement Immediately):**
1. ✅ JSON camelCase 規範 - 影響所有 API 通訊
2. ✅ 錯誤回應格式 - 影響錯誤處理一致性
3. ✅ Database naming - 影響 schema 設計

**Important (Implement Before Feature Development):**
4. ✅ 測試檔案組織 - 影響專案結構
5. ✅ 檔案命名規範 - 影響程式碼可維護性

**Good to Have (Improve Consistency):**
6. ✅ Loading state patterns - 提升 UX 一致性
7. ✅ Test naming conventions - 提升測試可讀性

---

## Next Steps

所有實作模式與一致性規則已定義完成,下一步將進入 **Step 6: Project Structure**(專案結構定義),定義完整的目錄結構與初始化步驟。

**Pattern Summary:**
- ✅ 25+ 個潛在衝突點已識別並定義明確規則
- ✅ 跨 3 種語言(Java, Python, TypeScript)的命名與格式一致性已確保
- ✅ 提供正確範例與反面教材,方便 AI agents 參考
- ✅ Code Review checklist 定義完成,可用於驗證實作

---

## Project Structure & Boundaries

_本章節定義完整的專案目錄結構與架構邊界,將所有需求映射至具體的檔案與目錄位置,為 AI agents 提供明確的實作指引。_

### Complete Project Directory Structure

```
lite-bank-demo/
├── README.md
├── .gitignore
├── LICENSE
├── Makefile                           # 一鍵部署、啟動、停止腳本
├── docker-compose.yml                 # 本地開發環境(PostgreSQL, Kafka)
│
├── docs/                              # 架構文件與支援文件
│   ├── architecture.md                # 架構決策文件(本文件)
│   ├── analysis/
│   │   └── product-brief-lite-bank-demo-2025-12-04.md
│   ├── database-access-rules.md       # 服務-表格存取權限矩陣
│   ├── error-codes.md                 # ERR_001 ~ ERR_0XX 錯誤碼對照表
│   ├── opentelemetry-conventions.md   # Span attributes 命名規範
│   ├── exception-hierarchy.md         # 自訂 Exception 類別階層
│   ├── api/                           # API 規格集中展示(可選)
│   │   ├── user-service.yaml
│   │   ├── account-service.yaml
│   │   ├── transfer-service.yaml
│   │   └── ...
│   └── diagrams/                      # 架構圖與流程圖
│       ├── architecture-overview.png
│       ├── saga-flow.png
│       └── trace-propagation.png
│
├── services/                          # 所有微服務
│   ├── api-gateway/                   # Java SpringBoot - API Gateway
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/gateway/
│   │       │   │   ├── GatewayApplication.java
│   │       │   │   ├── config/
│   │       │   │   │   ├── OTelConfig.java
│   │       │   │   │   ├── SecurityConfig.java
│   │       │   │   │   └── RouteConfig.java
│   │       │   │   ├── filter/
│   │       │   │   │   ├── TraceContextFilter.java
│   │       │   │   │   └── JwtAuthFilter.java
│   │       │   │   └── exception/
│   │       │   │       └── GlobalExceptionHandler.java
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── logback-spring.xml
│   │       └── test/
│   │           └── java/com/bank/gateway/
│   │               ├── filter/
│   │               └── config/
│   │
│   ├── user-service/                  # Java SpringBoot - 認證 & 使用者管理
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/user/
│   │       │   │   ├── UserServiceApplication.java
│   │       │   │   ├── controller/
│   │       │   │   │   ├── AuthController.java          # POST /api/v1/auth/login
│   │       │   │   │   └── UserController.java          # GET/PUT /api/v1/users/{userId}
│   │       │   │   ├── service/
│   │       │   │   │   ├── AuthService.java
│   │       │   │   │   ├── UserService.java
│   │       │   │   │   └── JwtService.java
│   │       │   │   ├── repository/
│   │       │   │   │   └── UserRepository.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── User.java
│   │       │   │   │   ├── LoginRequest.java
│   │       │   │   │   └── LoginResponse.java
│   │       │   │   ├── exception/
│   │       │   │   │   ├── UserNotFoundException.java
│   │       │   │   │   ├── InvalidCredentialsException.java
│   │       │   │   │   └── GlobalExceptionHandler.java
│   │       │   │   ├── config/
│   │       │   │   │   ├── OTelConfig.java
│   │       │   │   │   ├── SecurityConfig.java
│   │       │   │   │   └── DatabaseConfig.java
│   │       │   │   └── telemetry/
│   │       │   │       ├── TraceUtils.java
│   │       │   │       └── SpanAttributes.java
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       ├── logback-spring.xml
│   │       │       ├── db/migration/
│   │       │       │   └── V1__create_users_table.sql
│   │       │       └── db/seed/
│   │       │           └── R__seed_users.sql
│   │       └── test/
│   │           └── java/com/bank/user/
│   │               ├── controller/
│   │               ├── service/
│   │               └── repository/
│   │
│   ├── account-service/               # Java Quarkus - 帳戶管理
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile.jvm
│   │   ├── Dockerfile.native
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/account/
│   │       │   │   ├── resource/
│   │       │   │   │   └── AccountResource.java     # GET/POST /api/v1/accounts
│   │       │   │   ├── service/
│   │       │   │   │   └── AccountService.java
│   │       │   │   ├── repository/
│   │       │   │   │   └── AccountRepository.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── Account.java
│   │       │   │   │   ├── AccountRequest.java
│   │       │   │   │   └── AccountResponse.java
│   │       │   │   ├── exception/
│   │       │   │   │   ├── AccountNotFoundException.java
│   │       │   │   │   ├── InsufficientBalanceException.java
│   │       │   │   │   └── ExceptionMapper.java
│   │       │   │   ├── config/
│   │       │   │   │   └── OTelConfig.java
│   │       │   │   └── telemetry/
│   │       │   │       ├── TraceUtils.java
│   │       │   │       └── SpanAttributes.java
│   │       │   └── resources/
│   │       │       ├── application.properties
│   │       │       ├── db/migration/
│   │       │       │   └── V2__create_accounts_table.sql
│   │       │       └── db/seed/
│   │       │           └── R__seed_accounts.sql
│   │       └── test/
│   │           └── java/com/bank/account/
│   │               ├── resource/
│   │               ├── service/
│   │               └── repository/
│   │
│   ├── transaction-service/           # Java SpringBoot - 交易記錄查詢
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/transaction/
│   │       │   │   ├── TransactionServiceApplication.java
│   │       │   │   ├── controller/
│   │       │   │   │   └── TransactionController.java  # GET /api/v1/transactions
│   │       │   │   ├── service/
│   │       │   │   │   └── TransactionService.java
│   │       │   │   ├── repository/
│   │       │   │   │   └── TransactionRepository.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── Transaction.java
│   │       │   │   │   └── TransactionResponse.java
│   │       │   │   ├── exception/
│   │       │   │   │   └── GlobalExceptionHandler.java
│   │       │   │   ├── config/
│   │       │   │   │   └── OTelConfig.java
│   │       │   │   └── telemetry/
│   │       │   │       └── SpanAttributes.java
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       ├── logback-spring.xml
│   │       │       ├── db/migration/
│   │       │       │   └── V3__create_transactions_table.sql
│   │       │       └── db/seed/
│   │       └── test/
│   │           └── java/com/bank/transaction/
│   │
│   ├── deposit-withdrawal-service/    # Java SpringBoot - 存提款
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/deposit/
│   │       │   │   ├── DepositWithdrawalApplication.java
│   │       │   │   ├── controller/
│   │       │   │   │   ├── DepositController.java      # POST /api/v1/deposits
│   │       │   │   │   └── WithdrawalController.java   # POST /api/v1/withdrawals
│   │       │   │   ├── service/
│   │       │   │   │   ├── DepositService.java
│   │       │   │   │   └── WithdrawalService.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── DepositRequest.java
│   │       │   │   │   ├── WithdrawalRequest.java
│   │       │   │   │   └── OperationResponse.java
│   │       │   │   ├── exception/
│   │       │   │   │   ├── InsufficientBalanceException.java
│   │       │   │   │   └── GlobalExceptionHandler.java
│   │       │   │   ├── config/
│   │       │   │   │   └── OTelConfig.java
│   │       │   │   └── telemetry/
│   │       │   │       └── SpanAttributes.java
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── logback-spring.xml
│   │       └── test/
│   │           └── java/com/bank/deposit/
│   │
│   ├── transfer-service/              # Python FastAPI - SAGA 編排器(轉帳)
│   │   ├── README.md
│   │   ├── requirements.txt
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   ├── pyproject.toml
│   │   ├── .env.example
│   │   └── app/
│   │       ├── __init__.py
│   │       ├── main.py
│   │       ├── routers/
│   │       │   ├── __init__.py
│   │       │   └── transfer.py                # POST /api/v1/transfers
│   │       ├── services/
│   │       │   ├── __init__.py
│   │       │   ├── transfer_orchestrator.py   # SAGA 編排邏輯
│   │       │   ├── compensation_handler.py
│   │       │   └── saga_state_manager.py
│   │       ├── models/
│   │       │   ├── __init__.py
│   │       │   ├── transfer.py
│   │       │   ├── saga_execution.py
│   │       │   └── response.py
│   │       ├── exceptions/
│   │       │   ├── __init__.py
│   │       │   ├── transfer_exceptions.py
│   │       │   └── exception_handler.py
│   │       ├── database/
│   │       │   ├── __init__.py
│   │       │   ├── connection.py
│   │       │   ├── migrations/
│   │       │   │   └── V4__create_saga_executions_table.sql
│   │       │   └── seed/
│   │       ├── telemetry/
│   │       │   ├── __init__.py
│   │       │   ├── tracer.py
│   │       │   ├── span_attributes.py
│   │       │   └── propagation.py
│   │       ├── config/
│   │       │   ├── __init__.py
│   │       │   ├── settings.py
│   │       │   └── logging_config.py
│   │       └── clients/
│   │           ├── __init__.py
│   │           ├── account_client.py          # HTTP client to Account Service
│   │           ├── exchange_rate_client.py
│   │           ├── transaction_client.py
│   │           └── notification_client.py
│   │
│   ├── currency-exchange-service/     # Java Quarkus - 幣別兌換
│   │   ├── README.md
│   │   ├── pom.xml
│   │   ├── Dockerfile.jvm
│   │   ├── Dockerfile.native
│   │   ├── .dockerignore
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/bank/exchange/
│   │       │   │   ├── resource/
│   │       │   │   │   └── ExchangeResource.java   # POST /api/v1/exchanges
│   │       │   │   ├── service/
│   │       │   │   │   ├── ExchangeService.java
│   │       │   │   │   └── RateService.java
│   │       │   │   ├── model/
│   │       │   │   │   ├── ExchangeRequest.java
│   │       │   │   │   └── ExchangeResponse.java
│   │       │   │   ├── exception/
│   │       │   │   │   └── ExceptionMapper.java
│   │       │   │   ├── config/
│   │       │   │   │   └── OTelConfig.java
│   │       │   │   └── telemetry/
│   │       │   │       └── SpanAttributes.java
│   │       │   └── resources/
│   │       │       └── application.properties
│   │       └── test/
│   │           └── java/com/bank/exchange/
│   │
│   ├── exchange-rate-service/         # Python FastAPI - 匯率查詢(模擬外部 API)
│   │   ├── README.md
│   │   ├── requirements.txt
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   ├── pyproject.toml
│   │   ├── .env.example
│   │   └── app/
│   │       ├── __init__.py
│   │       ├── main.py
│   │       ├── routers/
│   │       │   ├── __init__.py
│   │       │   └── rates.py                   # GET /api/v1/rates/{from}/{to}
│   │       ├── services/
│   │       │   ├── __init__.py
│   │       │   └── rate_provider.py           # 模擬匯率資料來源
│   │       ├── models/
│   │       │   ├── __init__.py
│   │       │   └── rate.py
│   │       ├── exceptions/
│   │       │   ├── __init__.py
│   │       │   └── exception_handler.py
│   │       ├── telemetry/
│   │       │   ├── __init__.py
│   │       │   ├── tracer.py
│   │       │   └── span_attributes.py
│   │       ├── config/
│   │       │   ├── __init__.py
│   │       │   ├── settings.py
│   │       │   └── logging_config.py
│   │       └── data/
│   │           └── rates.json                 # 模擬匯率資料
│   │
│   └── notification-service/          # Python FastAPI - Kafka 消費者 + WebSocket
│       ├── README.md
│       ├── requirements.txt
│       ├── Dockerfile
│       ├── .dockerignore
│       ├── pyproject.toml
│       ├── .env.example
│       └── app/
│           ├── __init__.py
│           ├── main.py
│           ├── routers/
│           │   ├── __init__.py
│           │   └── websocket.py               # WebSocket /ws
│           ├── services/
│           │   ├── __init__.py
│           │   ├── kafka_consumer.py          # 消費 Kafka notification topic
│           │   ├── websocket_manager.py       # WebSocket 連接管理
│           │   └── notification_dispatcher.py
│           ├── models/
│           │   ├── __init__.py
│           │   └── notification.py
│           ├── exceptions/
│           │   ├── __init__.py
│           │   └── exception_handler.py
│           ├── telemetry/
│           │   ├── __init__.py
│           │   ├── tracer.py
│           │   ├── span_attributes.py
│           │   └── kafka_propagation.py       # Kafka trace context propagation
│           └── config/
│               ├── __init__.py
│               ├── settings.py
│               └── logging_config.py
│
├── frontend/                          # React + TypeScript
│   ├── README.md
│   ├── package.json
│   ├── package-lock.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── .env.example
│   ├── .env.local
│   ├── .gitignore
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── index.html
│   ├── public/
│   │   ├── favicon.ico
│   │   └── assets/
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── vite-env.d.ts
│       ├── pages/
│       │   ├── HomePage.tsx
│       │   ├── LoginPage.tsx
│       │   ├── AccountListPage.tsx
│       │   ├── AccountDetailPage.tsx
│       │   ├── TransferPage.tsx
│       │   ├── TransactionHistoryPage.tsx
│       │   └── CurrencyExchangePage.tsx
│       ├── components/
│       │   ├── ui/
│       │   │   ├── Button.tsx
│       │   │   ├── Input.tsx
│       │   │   ├── Card.tsx
│       │   │   ├── Loading.tsx
│       │   │   └── ErrorBoundary.tsx
│       │   ├── forms/
│       │   │   ├── LoginForm.tsx
│       │   │   ├── TransferForm.tsx
│       │   │   ├── DepositForm.tsx
│       │   │   └── WithdrawalForm.tsx
│       │   └── features/
│       │       ├── AccountCard.tsx
│       │       ├── TransactionList.tsx
│       │       ├── NotificationBadge.tsx
│       │       └── TraceIdDisplay.tsx         # 顯示 traceId 連結至 Grafana
│       ├── contexts/
│       │   ├── AuthContext.tsx
│       │   ├── AccountContext.tsx
│       │   └── NotificationContext.tsx
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useAccount.ts
│       │   ├── useWebSocket.ts
│       │   └── useTracing.ts
│       ├── services/
│       │   ├── api/
│       │   │   ├── client.ts                  # Axios instance with interceptors
│       │   │   ├── authApi.ts
│       │   │   ├── accountApi.ts
│       │   │   ├── transferApi.ts
│       │   │   ├── transactionApi.ts
│       │   │   └── exchangeApi.ts
│       │   └── websocket/
│       │       └── socketClient.ts            # Socket.io client
│       ├── telemetry/
│       │   ├── tracer.ts                      # OpenTelemetry Browser SDK setup
│       │   ├── spanAttributes.ts
│       │   └── exporter.ts
│       ├── types/
│       │   ├── account.ts
│       │   ├── transaction.ts
│       │   ├── transfer.ts
│       │   ├── user.ts
│       │   └── api.ts
│       ├── utils/
│       │   ├── formatters.ts                  # 金額、日期格式化
│       │   ├── validators.ts
│       │   └── errorHandlers.ts
│       ├── routes/
│       │   ├── AppRoutes.tsx
│       │   └── ProtectedRoute.tsx
│       ├── styles/
│       │   ├── global.css
│       │   └── theme.ts
│       └── assets/
│           └── images/
│
├── infrastructure/                    # K8s 部署配置
│   ├── README.md
│   ├── k8s/
│   │   ├── namespaces/
│   │   │   ├── namespace-services.yaml        # namespace: lite-bank-services
│   │   │   ├── namespace-infra.yaml           # namespace: lite-bank-infra
│   │   │   ├── namespace-observability.yaml   # namespace: lite-bank-observability
│   │   │   └── namespace-chaos.yaml           # namespace: chaos-mesh
│   │   ├── databases/
│   │   │   ├── postgresql-pvc.yaml
│   │   │   ├── postgresql-deployment.yaml
│   │   │   └── postgresql-service.yaml
│   │   ├── kafka/
│   │   │   ├── kafka-kraft-deployment.yaml    # Kafka KRaft mode
│   │   │   ├── kafka-service.yaml
│   │   │   ├── kafka-topics.yaml
│   │   │   └── kafka-ui-deployment.yaml
│   │   ├── observability/
│   │   │   ├── otel-collector/
│   │   │   │   ├── otel-collector-config.yaml         # Full Sampling 配置
│   │   │   │   ├── otel-collector-config-prob.yaml    # Probabilistic 配置
│   │   │   │   ├── otel-collector-config-tail.yaml    # Tail Sampling 配置
│   │   │   │   ├── otel-collector-deployment.yaml
│   │   │   │   └── otel-collector-service.yaml
│   │   │   ├── tempo/
│   │   │   │   ├── tempo-config.yaml
│   │   │   │   ├── tempo-deployment.yaml
│   │   │   │   └── tempo-service.yaml
│   │   │   ├── loki/
│   │   │   │   ├── loki-config.yaml
│   │   │   │   ├── loki-deployment.yaml
│   │   │   │   └── loki-service.yaml
│   │   │   ├── prometheus/
│   │   │   │   ├── prometheus-config.yaml
│   │   │   │   ├── prometheus-deployment.yaml
│   │   │   │   └── prometheus-service.yaml
│   │   │   └── grafana/
│   │   │       ├── grafana-config.yaml
│   │   │       ├── grafana-dashboards.yaml           # 預設儀表板
│   │   │       ├── grafana-datasources.yaml          # Tempo, Loki, Prometheus
│   │   │       ├── grafana-deployment.yaml
│   │   │       └── grafana-service.yaml
│   │   ├── services/                                  # 各微服務 K8s manifests
│   │   │   ├── api-gateway/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── user-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   ├── configmap.yaml
│   │   │   │   └── secret.yaml                       # JWT secret
│   │   │   ├── account-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── transaction-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── deposit-withdrawal-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── transfer-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── currency-exchange-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   ├── exchange-rate-service/
│   │   │   │   ├── deployment.yaml
│   │   │   │   ├── service.yaml
│   │   │   │   └── configmap.yaml
│   │   │   └── notification-service/
│   │   │       ├── deployment.yaml
│   │   │       ├── service.yaml
│   │   │       └── configmap.yaml
│   │   ├── chaos-mesh/
│   │   │   └── chaos-mesh-install.yaml                # Chaos Mesh operator 安裝
│   │   └── ingress/                                   # (可選) Ingress Controller
│   │       ├── ingress-nginx.yaml
│   │       └── ingress-rules.yaml
│   ├── helm/                                          # Helm Charts (可選,與 k8s/ 擇一)
│   │   └── lite-bank-demo/
│   │       ├── Chart.yaml
│   │       ├── values.yaml
│   │       ├── values-dev.yaml
│   │       ├── values-staging.yaml
│   │       ├── values-prod.yaml
│   │       └── templates/
│   │           ├── namespaces.yaml
│   │           ├── databases/
│   │           ├── kafka/
│   │           ├── observability/
│   │           └── services/
│   └── docker-compose/                                # 本地開發環境(非 K8s)
│       ├── docker-compose-infra.yml                   # PostgreSQL, Kafka, Grafana Stack
│       └── docker-compose-services.yml                # 所有微服務
│
├── chaos-scenarios/                                   # Chaos Mesh 場景定義
│   ├── README.md
│   ├── network-delay-exchange-rate.yaml               # NetworkChaos: 模擬匯率查詢延遲
│   ├── pod-failure-account-service.yaml               # PodChaos: Account Service 重啟
│   ├── stress-cpu-transfer-service.yaml               # StressChaos: Transfer Service CPU 100%
│   ├── network-partition-kafka.yaml                   # NetworkChaos: Kafka 網路分區
│   └── demo-scripts/
│       ├── 01-normal-transfer.sh                      # 正常轉帳操作
│       ├── 02-trigger-network-delay.sh                # 觸發網路延遲 Chaos
│       ├── 03-observe-compensation.sh                 # 觀察補償邏輯
│       └── 04-cleanup-chaos.sh                        # 清理 Chaos 場景
│
├── tests/                                             # 整合測試與端對端測試
│   ├── README.md
│   ├── integration/
│   │   ├── test_transfer_saga.py                      # SAGA 流程整合測試
│   │   ├── test_trace_propagation.py                  # Trace context propagation 測試
│   │   └── test_kafka_notification.py                 # Kafka + WebSocket 整合測試
│   ├── e2e/
│   │   ├── test_full_transfer_flow.py                 # 端對端轉帳流程測試
│   │   ├── test_chaos_scenarios.py                    # Chaos 場景測試
│   │   └── test_observability_integration.py          # 可觀測性整合測試
│   ├── performance/
│   │   ├── locustfile.py                              # Locust 壓測腳本
│   │   └── stress-test-config.yaml
│   └── fixtures/
│       ├── sample_accounts.json
│       └── sample_transactions.json
│
├── scripts/                                           # 開發與部署腳本
│   ├── README.md
│   ├── setup/
│   │   ├── install-k8s.sh                             # K8s cluster 初始化
│   │   ├── install-chaos-mesh.sh                      # Chaos Mesh 安裝
│   │   └── init-database.sh                           # PostgreSQL schema 初始化
│   ├── deploy/
│   │   ├── deploy-all.sh                              # 一鍵部署所有服務
│   │   ├── deploy-infra.sh                            # 部署基礎設施(PostgreSQL, Kafka)
│   │   ├── deploy-observability.sh                    # 部署 Grafana Stack
│   │   ├── deploy-services.sh                         # 部署微服務
│   │   └── deploy-frontend.sh                         # 部署 React 前端
│   ├── build/
│   │   ├── build-all.sh                               # 建置所有 Docker images
│   │   ├── build-java-services.sh                     # 建置 Java 服務
│   │   ├── build-python-services.sh                   # 建置 Python 服務
│   │   └── build-frontend.sh                          # 建置前端
│   ├── dev/
│   │   ├── start-local-infra.sh                       # 啟動本地開發環境(docker-compose)
│   │   ├── stop-local-infra.sh                        # 停止本地開發環境
│   │   └── reset-database.sh                          # 重置資料庫
│   └── monitoring/
│       ├── check-health.sh                            # 檢查所有服務健康狀態
│       ├── tail-logs.sh                               # 即時查看服務日誌
│       └── export-traces.sh                           # 匯出 traces 資料
│
├── .github/                                           # GitHub Actions CI/CD
│   └── workflows/
│       ├── ci-java-services.yml                       # Java 服務 CI
│       ├── ci-python-services.yml                     # Python 服務 CI
│       ├── ci-frontend.yml                            # 前端 CI
│       ├── build-and-push-images.yml                  # 建置並推送 Docker images
│       └── deploy-to-k8s.yml                          # 部署至 K8s
│
├── .vscode/                                           # VSCode 專案設定
│   ├── settings.json
│   ├── launch.json                                    # Debug configurations
│   └── extensions.json                                # 推薦擴充功能
│
└── .bmad/                                             # BMAD workflow 配置(內部使用)
    └── bmm/
        ├── config.yaml
        └── workflows/
```

### Architectural Boundaries

#### API Boundaries

**External API Endpoints (透過 API Gateway 或直接暴露):**

| Service | Endpoints | Authentication | Purpose |
|---------|-----------|----------------|---------|
| User Service | `POST /api/v1/auth/login`<br>`GET /api/v1/users/{userId}` | JWT (除了 login) | 使用者認證與管理 |
| Account Service | `GET /api/v1/accounts`<br>`GET /api/v1/accounts/{accountId}`<br>`POST /api/v1/accounts` | JWT | 帳戶查詢與建立 |
| Transaction Service | `GET /api/v1/transactions?accountId={accountId}` | JWT | 交易歷史查詢 |
| Deposit/Withdrawal Service | `POST /api/v1/deposits`<br>`POST /api/v1/withdrawals` | JWT | 存款與提款操作 |
| Transfer Service | `POST /api/v1/transfers` | JWT | 轉帳操作(SAGA 編排) |
| Currency Exchange Service | `POST /api/v1/exchanges` | JWT | 幣別兌換 |
| Exchange Rate Service | `GET /api/v1/rates/{from}/{to}` | - | 匯率查詢(模擬外部 API) |
| Notification Service | `WS /ws` | JWT (WebSocket auth) | WebSocket 推播通知 |

**Internal Service Communication (服務間同步呼叫):**

- Transfer Service → Account Service (`GET /api/v1/accounts/{accountId}`, `PUT /api/v1/accounts/{accountId}/balance`)
- Transfer Service → Exchange Rate Service (`GET /api/v1/rates/{from}/{to}`)
- Transfer Service → Transaction Service (`POST /api/v1/transactions`)
- All Services → User Service (`GET /api/v1/users/{userId}` for JWT validation, optional)

**Trace Context Propagation:**
所有 HTTP 呼叫必須傳遞 `traceparent` header:
```
traceparent: 00-{trace-id}-{span-id}-01
```

#### Component Boundaries

**Frontend Component Communication:**
- **Context API**: AuthContext, AccountContext, NotificationContext
- **Props Drilling**: 避免超過 2 層,優先使用 Context
- **Event Handlers**: 父子元件透過 callback props 通訊
- **WebSocket**: NotificationContext 統一管理 WebSocket 連線

**Backend Service Communication Patterns:**
- **Synchronous HTTP**: REST API 呼叫(Transfer Service → Account Service)
- **Asynchronous Messaging**: Kafka(Transfer Service → Notification Service)
- **Database Access**: 每個服務透過 Repository 層存取 PostgreSQL

**State Management Boundaries:**
- **Frontend State**: React Context API(使用者、帳戶、通知)
- **Backend State**: PostgreSQL(帳戶餘額、交易記錄、SAGA 狀態)
- **Caching**: Phase 1 不實作,Phase 2 可選 Redis

#### Service Boundaries

**Transfer Service (SAGA Orchestrator) Integration:**

Transfer Service 作為 SAGA 編排器,整合以下服務:

1. **Freeze Source Account** → Account Service
2. **Query Exchange Rate** → Exchange Rate Service
3. **Debit Source Account** → Account Service
4. **Credit Target Account** → Account Service
5. **Record Transaction** → Transaction Service
6. **Send Notification** → Kafka (notification topic)

**Kafka Topic Boundaries:**

| Topic | Producer | Consumer | Message Format |
|-------|----------|----------|----------------|
| `notification.transfer.success` | Transfer Service | Notification Service | JSON with `_trace` field |
| `notification.transfer.failure` | Transfer Service | Notification Service | JSON with `_trace` field |
| `notification.exchange.complete` | Currency Exchange Service | Notification Service | JSON with `_trace` field |

**Kafka Trace Context Propagation:**
Kafka 訊息 headers 包含:
```
traceparent: 00-{trace-id}-{span-id}-01
```

#### Data Boundaries

**PostgreSQL Schema Boundaries:**

| Table | Owner Service | Access Rights | Purpose |
|-------|---------------|---------------|---------|
| `users` | User Service | User(RW), Account(R), Transaction(R), Transfer(R) | 使用者資訊 |
| `accounts` | Account Service | Account(RW), Transfer(R), Deposit/Withdrawal(R) | 多幣別帳戶 |
| `transactions` | Transaction Service | Transaction(RW), Account(R), Transfer(R) | 交易記錄 |
| `saga_executions` | Transfer Service | Transfer(RW) | SAGA 狀態追蹤 |
| `exchange_rates` | Exchange Rate Service | Exchange Rate(RW), Currency Exchange(R), Transfer(R) | 匯率資料 |

**Database Access Rules (參考 `docs/database-access-rules.md`):**
- ✅ **允許**: 服務讀取其他服務的表格(唯讀)
- ❌ **禁止**: 服務寫入非自己擁有的表格
- ⚠️ **Code Review 必檢**: 確認無跨服務直接寫入

**Data Access Patterns:**
- **Read Pattern**: 透過 Repository interface 讀取資料
- **Write Pattern**: 僅寫入自己擁有的表格
- **Cross-Service Write**: 必須透過 HTTP API 呼叫,不可直接寫入資料庫

**Caching Boundaries (Phase 2):**
- Account balances: Redis cache(5 分鐘 TTL)
- Exchange rates: Redis cache(1 小時 TTL)
- User sessions: Redis(JWT token blacklist)

### Requirements to Structure Mapping

#### Feature/Epic Mapping

**Epic 1: User Management & Authentication**
- **Frontend**: [frontend/src/pages/LoginPage.tsx](frontend/src/pages/LoginPage.tsx), [frontend/src/contexts/AuthContext.tsx](frontend/src/contexts/AuthContext.tsx)
- **Backend**: [services/user-service/](services/user-service/)
- **Database**: `V1__create_users_table.sql`, `R__seed_users.sql`
- **API**: `POST /api/v1/auth/login`, `GET /api/v1/users/{userId}`
- **Tests**: [services/user-service/src/test/java/com/bank/user/](services/user-service/src/test/java/com/bank/user/)

**Epic 2: Account Management**
- **Frontend**: [frontend/src/pages/AccountListPage.tsx](frontend/src/pages/AccountListPage.tsx), [frontend/src/pages/AccountDetailPage.tsx](frontend/src/pages/AccountDetailPage.tsx)
- **Backend**: [services/account-service/](services/account-service/)
- **Database**: `V2__create_accounts_table.sql`, `R__seed_accounts.sql`
- **API**: `GET /api/v1/accounts`, `POST /api/v1/accounts`
- **Tests**: [services/account-service/src/test/java/com/bank/account/](services/account-service/src/test/java/com/bank/account/)

**Epic 3: Deposit & Withdrawal**
- **Frontend**: [frontend/src/components/forms/DepositForm.tsx](frontend/src/components/forms/DepositForm.tsx), [WithdrawalForm.tsx](frontend/src/components/forms/WithdrawalForm.tsx)
- **Backend**: [services/deposit-withdrawal-service/](services/deposit-withdrawal-service/)
- **API**: `POST /api/v1/deposits`, `POST /api/v1/withdrawals`
- **Tests**: [services/deposit-withdrawal-service/src/test/](services/deposit-withdrawal-service/src/test/)

**Epic 4: Transfer (SAGA Pattern)**
- **Frontend**: [frontend/src/pages/TransferPage.tsx](frontend/src/pages/TransferPage.tsx)
- **Backend**: [services/transfer-service/](services/transfer-service/)
- **Database**: `V4__create_saga_executions_table.sql`
- **API**: `POST /api/v1/transfers`
- **Tests**: [tests/integration/test_transfer_saga.py](tests/integration/test_transfer_saga.py)
- **Observability**: 所有 6 個 SAGA 步驟在 Grafana Tempo 可視化

**Epic 5: Currency Exchange**
- **Frontend**: [frontend/src/pages/CurrencyExchangePage.tsx](frontend/src/pages/CurrencyExchangePage.tsx)
- **Backend**: [services/currency-exchange-service/](services/currency-exchange-service/), [services/exchange-rate-service/](services/exchange-rate-service/)
- **API**: `POST /api/v1/exchanges`, `GET /api/v1/rates/{from}/{to}`
- **Tests**: [services/currency-exchange-service/src/test/](services/currency-exchange-service/src/test/)

**Epic 6: Transaction History**
- **Frontend**: [frontend/src/pages/TransactionHistoryPage.tsx](frontend/src/pages/TransactionHistoryPage.tsx)
- **Backend**: [services/transaction-service/](services/transaction-service/)
- **Database**: `V3__create_transactions_table.sql`
- **API**: `GET /api/v1/transactions?accountId={accountId}`
- **Tests**: [services/transaction-service/src/test/](services/transaction-service/src/test/)

**Epic 7: Notification System**
- **Frontend**: [frontend/src/contexts/NotificationContext.tsx](frontend/src/contexts/NotificationContext.tsx), [frontend/src/components/features/NotificationBadge.tsx](frontend/src/components/features/NotificationBadge.tsx)
- **Backend**: [services/notification-service/](services/notification-service/)
- **Messaging**: Kafka topics(`notification.transfer.success`, `notification.transfer.failure`)
- **WebSocket**: `WS /ws`
- **Tests**: [tests/integration/test_kafka_notification.py](tests/integration/test_kafka_notification.py)

**Epic 8: Observability Integration**
- **All Services**: `*/telemetry/` directory (TraceUtils, SpanAttributes, Tracer)
- **Infrastructure**: [infrastructure/k8s/observability/](infrastructure/k8s/observability/)
- **Documentation**: [docs/opentelemetry-conventions.md](docs/opentelemetry-conventions.md)
- **Tests**: [tests/e2e/test_observability_integration.py](tests/e2e/test_observability_integration.py)

**Epic 9: Chaos Engineering**
- **Scenarios**: [chaos-scenarios/*.yaml](chaos-scenarios/)
- **Scripts**: [chaos-scenarios/demo-scripts/](chaos-scenarios/demo-scripts/)
- **Infrastructure**: [infrastructure/k8s/chaos-mesh/](infrastructure/k8s/chaos-mesh/)
- **Tests**: [tests/e2e/test_chaos_scenarios.py](tests/e2e/test_chaos_scenarios.py)

#### Cross-Cutting Concerns

**Authentication & Authorization**
- **JWT Token Generation**: `services/user-service/src/main/java/com/bank/user/service/JwtService.java`
- **JWT Validation**: 所有服務的 `SecurityConfig.java` 或 `auth_middleware.py`
- **Frontend Token Storage**: [frontend/src/contexts/AuthContext.tsx](frontend/src/contexts/AuthContext.tsx)
- **Secret Management**: [infrastructure/k8s/services/user-service/secret.yaml](infrastructure/k8s/services/user-service/secret.yaml)

**OpenTelemetry Instrumentation**
- **Java Services**: `*/config/OTelConfig.java`, `*/telemetry/TraceUtils.java`, `*/telemetry/SpanAttributes.java`
- **Python Services**: `*/telemetry/tracer.py`, `*/telemetry/span_attributes.py`, `*/telemetry/propagation.py`
- **Frontend**: [frontend/src/telemetry/tracer.ts](frontend/src/telemetry/tracer.ts), [frontend/src/telemetry/exporter.ts](frontend/src/telemetry/exporter.ts)
- **Conventions**: [docs/opentelemetry-conventions.md](docs/opentelemetry-conventions.md)

**Error Handling**
- **Java**: `*/exception/GlobalExceptionHandler.java`, `*/exception/ExceptionMapper.java`
- **Python**: `*/exceptions/exception_handler.py`
- **Frontend**: [frontend/src/utils/errorHandlers.ts](frontend/src/utils/errorHandlers.ts), [frontend/src/components/ui/ErrorBoundary.tsx](frontend/src/components/ui/ErrorBoundary.tsx)
- **Error Codes**: [docs/error-codes.md](docs/error-codes.md)
- **Exception Hierarchy**: [docs/exception-hierarchy.md](docs/exception-hierarchy.md)

**Structured Logging**
- **Java**: `*/resources/logback-spring.xml`
- **Python**: `*/config/logging_config.py`
- **Log Format**: JSON with `traceId`, `spanId`, `service.name`, `level`, `message`, `timestamp`

**Database Migrations**
- **Java Services**: `src/main/resources/db/migration/V{version}__{description}.sql`
- **Python Services**: `app/database/migrations/V{version}__{description}.sql`
- **Seed Data**: `src/main/resources/db/seed/R__seed_{table}.sql`

**Configuration Management**
- **Java SpringBoot**: `application.yml`
- **Java Quarkus**: `application.properties`
- **Python FastAPI**: `.env` + `config/settings.py`
- **Frontend**: `.env.local`
- **K8s**: `ConfigMap` 與 `Secret`

### Integration Points

#### Internal Communication

**Synchronous HTTP Communication (REST API):**
- **Transfer Service → Account Service**: HTTP GET/PUT for account balance operations
  - Headers: `Authorization: Bearer {jwt}`, `traceparent: 00-{trace-id}-{span-id}-01`
  - Timeout: 5 seconds
  - Retry: 3 次(指數退避)
- **Transfer Service → Exchange Rate Service**: HTTP GET for exchange rates
  - Headers: `traceparent`
  - Timeout: 3 seconds
  - Retry: 3 次
- **All Services → User Service**: HTTP GET for user validation (optional)
  - Cache JWT validation results(避免每次請求都驗證)

**Asynchronous Messaging (Kafka):**
- **Transfer Service → Notification Service**: Kafka topic `notification.transfer.success`
  - Message Format: JSON with `_trace` field
  - Headers: `traceparent`
  - Delivery Guarantee: At-least-once
- **Currency Exchange Service → Notification Service**: Kafka topic `notification.exchange.complete`

**WebSocket Communication:**
- **Notification Service → Frontend**: WebSocket `WS /ws`
  - Authentication: JWT token in connection handshake
  - Message Format: JSON with `type`, `data`, `_trace`
  - Heartbeat: 每 30 秒
  - Auto-reconnect: 指數退避(1s, 2s, 4s, 8s, max 30s)

#### External Integrations

**Mock External Services:**
- **Exchange Rate Service**: 模擬第三方匯率查詢 API
  - Endpoint: `GET /api/v1/rates/{from}/{to}`
  - Response Time: 可配置延遲(用於 Chaos 測試)
  - Data Source: [services/exchange-rate-service/app/data/rates.json](services/exchange-rate-service/app/data/rates.json)

**Grafana Stack:**
- **Tempo**: Trace data 接收(從 OTel Collector)
  - Endpoint: `http://tempo:4317` (OTLP gRPC)
- **Loki**: Log data 接收(從 OTel Collector)
  - Endpoint: `http://loki:3100/loki/api/v1/push`
- **Prometheus**: Metrics scraping
  - Scrape Interval: 15 秒
  - Targets: 所有微服務的 `/metrics` endpoint
- **Grafana**: 統一查詢介面
  - Datasources: Tempo, Loki, Prometheus
  - Dashboards: 預設儀表板(service overview, SAGA flow, error rates)

**OpenTelemetry Collector:**
- **Receivers**: OTLP gRPC(4317), OTLP HTTP(4318)
- **Processors**: batch, memory_limiter
- **Exporters**: 
  - Tempo(traces) → `otlp/tempo`
  - Loki(logs) → `loki`
  - Prometheus(metrics) → `prometheus`
- **Sampling**: 透過 ConfigMap 切換(Full, Probabilistic, Tail)

#### Data Flow

**Normal Transfer Flow (SAGA Happy Path):**

1. **Frontend** → `POST /api/v1/transfers` → **Transfer Service**
   - Headers: `Authorization`, `traceparent`
2. **Transfer Service** → SAGA 開始,建立 `saga_executions` 記錄(status: `IN_PROGRESS`)
3. **Transfer Service** → `GET /api/v1/accounts/{sourceAccountId}` → **Account Service**
   - Headers: `traceparent`
4. **Transfer Service** → `GET /api/v1/rates/TWD/USD` → **Exchange Rate Service**
   - Headers: `traceparent`
5. **Transfer Service** → `PUT /api/v1/accounts/{sourceAccountId}/balance` → **Account Service**(扣款)
   - Headers: `traceparent`
6. **Transfer Service** → `PUT /api/v1/accounts/{targetAccountId}/balance` → **Account Service**(入帳)
   - Headers: `traceparent`
7. **Transfer Service** → `POST /api/v1/transactions` → **Transaction Service**(記錄交易)
   - Headers: `traceparent`
8. **Transfer Service** → Kafka `notification.transfer.success` → **Notification Service**
   - Message Headers: `traceparent`
9. **Notification Service** → WebSocket `WS /ws` → **Frontend**(推播通知)
   - Payload: `{ type: "TRANSFER_SUCCESS", data: {...}, _trace: {...} }`
10. **Transfer Service** → 更新 `saga_executions` 記錄(status: `COMPLETED`)
11. **Frontend** → 顯示成功訊息,更新帳戶餘額

**Trace Context Propagation Path:**
```
Frontend(traceId: abc123)
  ↓ traceparent header
Transfer Service(traceId: abc123, spanId: def456)
  ↓ traceparent header
Account Service(traceId: abc123, spanId: ghi789)
  ↓ Kafka header
Notification Service(traceId: abc123, spanId: jkl012)
  ↓ WebSocket message
Frontend(traceId: abc123, display in UI)
```

**Error Flow (SAGA Compensation):**

1. **Transfer Service** → Step 3 失敗(例如:扣款時餘額不足)
2. **Transfer Service** → 觸發補償邏輯:
   - 解凍來源帳戶(如果 Step 1 已執行)
3. **Transfer Service** → 更新 `saga_executions` 記錄(status: `COMPENSATED`)
4. **Transfer Service** → Kafka `notification.transfer.failure` → **Notification Service**
5. **Notification Service** → WebSocket → **Frontend**(推播失敗通知)
6. **Frontend** → 顯示錯誤訊息(包含 `traceId` 連結至 Grafana Tempo)

**Observability Data Flow:**

1. **所有服務** → OpenTelemetry SDK → Traces, Logs, Metrics
2. **Services** → OTel Collector(OTLP gRPC 4317)
3. **OTel Collector** → Tempo(traces), Loki(logs), Prometheus(metrics)
4. **Grafana** → 查詢 Tempo, Loki, Prometheus
5. **SRE/Developer** → Grafana UI → 從任一 log/trace/metric 跳轉至其他支柱

### File Organization Patterns

#### Configuration Files

**Root-Level Configuration:**
- `docker-compose.yml`: 本地開發環境(PostgreSQL, Kafka, Grafana Stack)
- `Makefile`: 一鍵指令(build, deploy, start, stop, test)
- `.gitignore`: 忽略 `node_modules/`, `target/`, `__pycache__/`, `.env.local`

**Service-Level Configuration:**
- **Java SpringBoot**: `src/main/resources/application.yml`
  - Profiles: `dev`, `staging`, `prod`
  - Example: `spring.profiles.active=dev`
- **Java Quarkus**: `src/main/resources/application.properties`
  - Profiles: `%dev`, `%staging`, `%prod`
- **Python FastAPI**: `.env` + `app/config/settings.py`
  - Environment variables: `DATABASE_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `OTEL_EXPORTER_OTLP_ENDPOINT`
- **React**: `.env.local`
  - Variables: `VITE_API_BASE_URL`, `VITE_WS_URL`, `VITE_OTEL_ENDPOINT`

**K8s Configuration:**
- **ConfigMap**: 環境變數(非敏感)
  - `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317`
  - `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- **Secret**: 敏感資訊
  - `JWT_SECRET=base64encoded`
  - `DATABASE_PASSWORD=base64encoded`

#### Source Organization

**Java Services Structure:**
```
src/main/java/com/bank/{service}/
├── {Service}Application.java         # Spring Boot entry point
├── controller/                        # REST API endpoints
├── service/                           # Business logic
├── repository/                        # Database access
├── model/                             # Domain models, DTOs
├── exception/                         # Custom exceptions + GlobalExceptionHandler
├── config/                            # Spring configuration(OTel, Security, Database)
└── telemetry/                         # OpenTelemetry utilities(TraceUtils, SpanAttributes)
```

**Python Services Structure:**
```
app/
├── main.py                            # FastAPI entry point
├── routers/                           # API endpoints
├── services/                          # Business logic
├── models/                            # Pydantic models
├── database/                          # Database connection, migrations
├── exceptions/                        # Custom exceptions + exception_handler
├── telemetry/                         # OpenTelemetry setup(tracer, span_attributes)
├── config/                            # Settings, logging config
└── clients/                           # HTTP clients(optional)
```

**React Frontend Structure:**
```
frontend/src/
├── main.tsx                           # Entry point
├── App.tsx                            # Root component
├── pages/                             # Page components
├── components/                        # Reusable components(ui/, forms/, features/)
├── contexts/                          # React Context providers
├── hooks/                             # Custom hooks
├── services/                          # API clients, WebSocket
├── telemetry/                         # OpenTelemetry Browser SDK
├── types/                             # TypeScript type definitions
├── utils/                             # Helper functions
├── routes/                            # React Router configuration
└── styles/                            # Global styles, theme
```

#### Test Organization

**Java Services (Separate Directory):**
```
src/test/java/com/bank/{service}/
├── controller/                        # Controller unit tests
├── service/                           # Service unit tests
└── repository/                        # Repository integration tests
```

**Python Services (Separate Directory):**
```
tests/
├── unit/
│   ├── test_transfer_orchestrator.py
│   └── test_saga_state_manager.py
├── integration/
│   └── test_database_access.py
└── conftest.py                        # Pytest fixtures
```

**React Frontend (Flexible):**
```
frontend/src/
└── components/
    ├── AccountCard.tsx
    └── AccountCard.test.tsx           # Co-located test file
```

**End-to-End Tests (Repository Root):**
```
tests/
├── integration/                       # Cross-service integration tests
├── e2e/                               # Full user flow tests
├── performance/                       # Load tests
└── fixtures/                          # Test data
```

#### Asset Organization

**Static Assets:**
- **Frontend**: [frontend/public/assets/](frontend/public/assets/)
  - Images: `images/`
  - Fonts: `fonts/`
  - Icons: `icons/`

**Documentation Assets:**
- **Architecture Diagrams**: [docs/diagrams/](docs/diagrams/)
  - PNG, SVG, or Excalidraw files

**Configuration Assets:**
- **OTel Collector Configs**: [infrastructure/k8s/observability/otel-collector/](infrastructure/k8s/observability/otel-collector/)
  - `otel-collector-config.yaml`(Full Sampling)
  - `otel-collector-config-prob.yaml`(Probabilistic)
  - `otel-collector-config-tail.yaml`(Tail Sampling)

### Development Workflow Integration

#### Development Server Structure

**Local Development (docker-compose):**

1. **啟動基礎設施**:
   ```bash
   docker-compose -f infrastructure/docker-compose/docker-compose-infra.yml up -d
   ```
   - PostgreSQL → `localhost:5432`
   - Kafka → `localhost:9092`
   - Grafana → `localhost:3000`
   - Tempo → `localhost:3100`

2. **初始化資料庫**:
   ```bash
   ./scripts/dev/reset-database.sh
   ```
   - 執行所有 Flyway migrations
   - 執行 seed data scripts

3. **啟動微服務**(本地開發):
   - **Java Services**: IDE 或 `mvn spring-boot:run`
   - **Python Services**: `uvicorn app.main:app --reload`
   - **Frontend**: `npm run dev`

4. **監控服務**:
   - Grafana: `http://localhost:3000`
   - Kafka UI: `http://localhost:8080`

**K8s Development (minikube/kind):**

1. **建立 K8s cluster**:
   ```bash
   ./scripts/setup/install-k8s.sh
   ```

2. **部署所有服務**:
   ```bash
   ./scripts/deploy/deploy-all.sh
   ```
   - 建置所有 Docker images
   - 部署基礎設施、可觀測性工具、微服務

3. **Port-forward 服務**:
   ```bash
   kubectl port-forward -n lite-bank-services svc/api-gateway 8080:8080
   kubectl port-forward -n lite-bank-observability svc/grafana 3000:3000
   ```

#### Build Process Structure

**Multi-Stage Docker Build:**

**Java Services Example:**
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Python Services Example:**
```dockerfile
# Build stage
FROM python:3.11-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Runtime stage
FROM python:3.11-slim
WORKDIR /app
COPY --from=builder /root/.local /root/.local
COPY app ./app
ENV PATH=/root/.local/bin:$PATH
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Frontend Build:**
```dockerfile
# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage (Nginx)
FROM nginx:1.25-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Nginx Configuration Details:**

```nginx
# frontend/nginx.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 壓縮 (提升傳輸效能)
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # SPA 路由支援 (所有路由返回 index.html)
    location / {
        try_files $uri $uri/ /index.html;
        # 禁止快取 index.html (確保使用者獲得最新版本)
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # 靜態資源快取策略 (提升載入速度)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # API 代理 (Phase 1 必備,代理至 API Gateway)
    location /api/ {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 傳遞 trace context (如果前端已建立)
        proxy_set_header traceparent $http_traceparent;

        # 傳遞 Request ID
        proxy_set_header X-Request-ID $request_id;
    }

    # WebSocket 代理 (支援 Notification Service)
    location /ws {
        proxy_pass http://notification-service:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # WebSocket 超時設定 (改為合理值:5分鐘無活動)
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;

        # 傳遞 Request ID (用於 trace 關聯)
        proxy_set_header X-Request-ID $request_id;

        # Origin 驗證 (Phase 2 生產環境強制啟用)
        # if ($http_origin !~ '^https?://(localhost|lite-bank-demo\.example\.com)') {
        #     return 403;
        # }
    }

    # 健康檢查端點 (K8s liveness probe)
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    # 安全標頭 (完整安全防護)
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 新增:強制 HTTPS (生產環境啟用)
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 新增:Content Security Policy
    add_header Content-Security-Policy "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; font-src 'self' data:; connect-src 'self' ws: wss: http://api-gateway:8080;" always;

    # 新增:Referrer Policy
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 新增:Permissions Policy (禁止不必要的瀏覽器功能)
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
}
```

**Nginx 配置關鍵點:**

1. **SPA 路由支援**: `try_files $uri $uri/ /index.html` 確保 React Router 正常運作
2. **WebSocket 代理**: Nginx 1.3+ 支援 WebSocket,配置 `Upgrade` 與 `Connection` headers
3. **快取策略**:
   - `index.html`: 無快取 (確保版本更新)
   - 靜態資源: 1 年長快取 (hash 檔名變更時自動失效)
4. **Gzip 壓縮**: 減少傳輸大小,提升載入速度
5. **健康檢查**: `/health` 端點供 K8s liveness probe 使用

**K8s Integration:**

```yaml
# infrastructure/k8s/services/frontend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: lite-bank-services
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: lite-bank/frontend:latest
        ports:
        - containerPort: 80
        livenessProbe:
          httpGet:
            path: /health
            port: 80
          initialDelaySeconds: 10
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /health
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 10
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
```

**Affects:** Frontend deployment, SPA routing, WebSocket connectivity, K8s health checks

**Build Automation:**
```bash
./scripts/build/build-all.sh
```
- 建置所有 Java services(Maven)
- 建置所有 Python services(Docker)
- 建置 React frontend(npm + Docker)
- Tag images: `lite-bank/{service}:latest`

#### Deployment Structure

**Helm Deployment (推薦):**

```bash
helm install lite-bank-demo ./infrastructure/helm/lite-bank-demo \
  --namespace lite-bank-services \
  --create-namespace \
  --values ./infrastructure/helm/lite-bank-demo/values-dev.yaml
```

**直接 K8s Manifests:**

```bash
kubectl apply -f infrastructure/k8s/namespaces/
kubectl apply -f infrastructure/k8s/databases/
kubectl apply -f infrastructure/k8s/kafka/
kubectl apply -f infrastructure/k8s/observability/
kubectl apply -f infrastructure/k8s/services/
```

**部署順序:**
1. Namespaces
2. PostgreSQL
3. Kafka
4. Grafana Stack(Tempo, Loki, Prometheus, Grafana)
5. OTel Collector
6. User Service(優先,其他服務依賴 JWT 驗證)
7. Account Service, Exchange Rate Service
8. Transaction Service, Deposit/Withdrawal Service
9. Transfer Service(依賴上述服務)
10. Notification Service
11. API Gateway
12. Frontend

**健康檢查:**
```bash
./scripts/monitoring/check-health.sh
```
- 檢查所有 pods 是否 Running
- 檢查所有 services 是否 Ready
- 驗證 Grafana datasources 連線

---

## Next Steps

完整的專案結構已定義完成,所有需求已映射至具體的檔案與目錄位置,下一步將進入 **Step 7: Architecture Validation**(架構驗證),確保所有決策的一致性、完整性與可實作性。

**Structure Summary:**
- ✅ 10 個微服務的完整目錄結構已定義
- ✅ React 前端的完整組織架構已規劃
- ✅ K8s 部署配置的完整層次已建立
- ✅ 所有 Epics 已映射至具體檔案位置
- ✅ 跨領域關注點(Authentication, Telemetry, Error Handling)已明確定位
- ✅ 開發、建置、部署工作流程已整合

**Pending Actions:**
1. 初始化專案目錄結構(`mkdir -p services/{service-name}/...`)
2. 建立 K8s manifests 基本檔案
3. 準備 Chaos 場景腳本
4. 建立開發與部署腳本
5. 進入 Step 7: 架構驗證


---

## Architecture Validation Results

_本章節記錄完整的架構驗證結果,確認所有決策的一致性、需求覆蓋度與實作就緒性。_

### Coherence Validation ✅

**Decision Compatibility (決策相容性):**

所有架構決策經過審查,確認無技術衝突或不一致:

- ✅ **API Gateway + 微服務整合**: Spring Cloud Gateway 3.1.x 與 SpringBoot 微服務完全相容,支援 K8s Service Discovery
- ✅ **前端技術棧**: React 18 + Material-UI v5.15.0 + TypeScript + Vite 全部相容,無版本衝突
- ✅ **OpenTelemetry 跨語言**: Java (1.x), Python (1.x), Browser SDK (1.x) 版本對齊,trace context 格式一致
- ✅ **資料庫整合**: PostgreSQL + Flyway 10.x 支援所有三種語言 (Java JDBC, Python psycopg, FastAPI SQLAlchemy)
- ✅ **訊息佇列**: Kafka KRaft 模式與 Spring Kafka, kafka-python 客戶端相容
- ✅ **容器化**: Docker multi-stage build 適用於 Java (Maven), Python (pip), Node.js (npm)
- ✅ **Nginx 配置**: Nginx 1.25-alpine 支援 WebSocket proxy, API proxy, SPA routing

**Pattern Consistency (模式一致性):**

實作模式經過驗證,確保跨語言、跨服務一致性:

- ✅ **JSON 命名**: 統一 camelCase,Python 透過 Pydantic `Field(alias="...")` 轉換
- ✅ **資料庫命名**: 統一 snake_case (tables, columns, indexes)
- ✅ **API 命名**: kebab-case 資源路徑 (`/api/v1/accounts`), camelCase 參數
- ✅ **程式碼命名**: 
  - Java: PascalCase 類別, camelCase 方法/變數
  - Python: snake_case 檔案/函數, PascalCase 類別
  - React: PascalCase 元件檔案 (`AccountCard.tsx`)
- ✅ **錯誤格式**: 統一 JSON 結構 + ERR_001 數字錯誤碼
- ✅ **日期格式**: 統一 ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ss.sssZ`)
- ✅ **Trace propagation**: 統一使用 `traceparent` header (HTTP + Kafka)

**Structure Alignment (結構對齊):**

專案結構經過審查,確認支援所有架構決策:

- ✅ **API Gateway 統一入口**: 前端透過 Nginx `/api/` proxy 至 Gateway,簡化配置
- ✅ **微服務目錄結構**: Java (controller/service/repository), Python (routers/services/models) 符合框架慣例
- ✅ **測試目錄分離**: Java (`src/test/`), Python (`tests/`) 符合決策
- ✅ **K8s Namespace 隔離**: 4 個 namespace 邊界清晰 (services, infra, observability, chaos)
- ✅ **Telemetry 目錄**: 所有服務包含 `*/telemetry/` 目錄,統一 OpenTelemetry instrumentation

### Requirements Coverage Validation ✅

**Epic/Feature Coverage (Epic 覆蓋度):**

所有 9 個 Epic 均有完整的架構支援:

| Epic | 架構支援 | 關鍵元件 | 狀態 |
|------|----------|----------|------|
| 1. User Management & Authentication | ✅ 完整 | User Service (JWT), AuthContext | 已映射 |
| 2. Account Management | ✅ 完整 | Account Service (Quarkus), AccountListPage | 已映射 |
| 3. Deposit & Withdrawal | ✅ 完整 | Deposit/Withdrawal Service | 已映射 |
| 4. Transfer (SAGA) | ✅ 完整 | Transfer Service (Python), SAGA 補償邏輯 | 已映射 |
| 5. Currency Exchange | ✅ 完整 | Currency Exchange Service + Exchange Rate Service | 已映射 |
| 6. Transaction History | ✅ 完整 | Transaction Service, TransactionHistoryPage | 已映射 |
| 7. Notification System | ✅ 完整 | Notification Service (Kafka + WebSocket) | 已映射 |
| 8. Observability Integration | ✅ 完整 | OTel Collector + Grafana Stack (Tempo/Loki/Prometheus) | 已映射 |
| 9. Chaos Engineering | ✅ 完整 | Chaos Mesh + 4 scenarios (network/pod/stress/partition) | 已映射 |

**Functional Requirements Coverage (功能需求覆蓋):**

- ✅ **多幣別帳戶**: PostgreSQL `accounts` 表支援 TWD/USD/JPY,Account Service 完整實作
- ✅ **存提款操作**: Deposit/Withdrawal Service 原子性操作
- ✅ **轉帳功能**: Transfer Service SAGA 編排器,6 步驟流程 + 補償邏輯
- ✅ **換匯功能**: Currency Exchange Service (Quarkus) + Exchange Rate Service (Python mock)
- ✅ **交易歷史**: Transaction Service 提供查詢 API
- ✅ **使用者認證**: User Service JWT 認證,API Gateway 集中驗證

**Non-Functional Requirements Coverage (非功能需求覆蓋):**

| NFR | 架構支援 | 實作方式 |
|-----|----------|----------|
| **可觀測性完整性** | ✅ 完整 | Frontend → Gateway → 微服務 → Kafka → WebSocket 完整 trace propagation |
| **跨語言 trace propagation** | ✅ 完整 | 手動 SDK instrumentation (Java/Python/Browser), `traceparent` header 傳遞 |
| **自定義 span attributes** | ✅ 完整 | `docs/opentelemetry-conventions.md` 定義業務語義 (account.id, transaction.amount) |
| **log ↔ trace ↔ metric 跳轉** | ✅ 完整 | 統一 traceId/spanId 關聯,Grafana datasources 整合 |
| **技術真實性** | ✅ 完整 | 同步轉帳 (HTTP), Kafka 僅用於通知, Chaos Mesh 真實故障 |
| **Demo 展示需求** | ✅ 完整 | 一鍵部署 (`deploy-all.sh`), 美觀 UI (MUI v5), Chaos 即時觸發 |
| **效能與可靠性** | ✅ 完整 | SAGA 補償邏輯, Level 2 錯誤分類 (business/external/system) |

### Implementation Readiness Validation ✅

**Decision Completeness (決策完整性):**

所有關鍵決策已文件化並包含版本資訊:

- ✅ **資料庫遷移工具**: Flyway 10.x (SQL-based migrations)
- ✅ **微服務資料庫策略**: Shared Schema + `docs/database-access-rules.md`
- ✅ **API Gateway**: Spring Cloud Gateway 3.1.x (Phase 1 必備)
- ✅ **API 版本控制**: URL Path Versioning (`/api/v{version}/`)
- ✅ **錯誤處理格式**: 統一 JSON + ERR_001 數字錯誤碼
- ✅ **K8s 部署工具**: Helm Charts (推薦) 或直接 manifests
- ✅ **OTel Collector 採樣**: 彈性配置 (Full/Probabilistic/Tail Sampling)
- ✅ **前端 UI 框架**: Material-UI v5.15.0 + @emotion
- ✅ **前端容器化**: Nginx 1.25-alpine + multi-stage build

**實作範例完整性:**

每個關鍵決策都包含可執行的程式碼範例:

- ✅ **Pydantic Aliases**: 完整 Python 範例展示 snake_case → camelCase 轉換
- ✅ **Spring Cloud Gateway 配置**: 完整 YAML 配置,包含所有 8 個微服務路由
- ✅ **SAGA 狀態表**: 完整 SQL schema + 狀態轉換表
- ✅ **錯誤回應格式**: 完整 JSON 結構 + HTTP 狀態碼對照
- ✅ **Nginx 配置**: 完整 server block (SPA routing, WebSocket proxy, 安全標頭)
- ✅ **MUI Theme 配置**: 完整 TypeScript 設定 + 元件使用範例
- ✅ **Bundle 優化**: Code Splitting, Icon 按需載入, Vite manualChunks 配置

**Structure Completeness (結構完整性):**

專案結構完整且具體,非通用模板:

- ✅ **10 個微服務**: 每個服務都有完整的目錄樹 (controller/service/repository/model/exception/config/telemetry)
- ✅ **Frontend**: 完整 React 結構 (pages/components/contexts/hooks/services/telemetry/types/utils)
- ✅ **Infrastructure**: K8s manifests 完整定義 (namespaces/databases/kafka/observability/services)
- ✅ **Chaos Scenarios**: 4 個 YAML 場景 + demo scripts
- ✅ **Scripts**: setup/deploy/build/dev/monitoring 腳本完整規劃
- ✅ **Tests**: integration/e2e/performance 測試結構定義

**Pattern Completeness (模式完整性):**

實作模式涵蓋所有潛在衝突點:

- ✅ **25+ 個衝突點已解決**: 命名、結構、格式、通訊、流程五大類
- ✅ **Code Review Checklist**: 8 項檢查清單可執行
- ✅ **Good/Bad Examples**: 每個模式都有正確與錯誤範例對照
- ✅ **優化策略**: Bundle 優化 (Code Splitting), 安全強化 (CSP, HSTS), 效能目標 (Core Web Vitals)

### Gap Analysis Results

**Critical Gaps (關鍵缺口): 0 個**

無阻擋實作的架構缺口。所有必要決策已完成。

**Important Gaps (重要缺口): 4 個**

以下文件為實作產物,架構文件已提供足夠指引,可在開發時建立:

1. **`docs/database-access-rules.md`**
   - 內容:服務-表格存取權限矩陣 (RW/R 權限)
   - 建立時機:User Service, Account Service 實作時
   - 架構指引:已提供矩陣範例 (章節 1.3)

2. **`docs/error-codes.md`**
   - 內容:ERR_001 ~ ERR_0XX 完整對照表
   - 建立時機:實作各微服務時逐步擴充
   - 架構指引:已提供錯誤碼範例與分類規則 (章節 3.3)

3. **`docs/opentelemetry-conventions.md`**
   - 內容:Span attributes 命名規範 (account.id, transaction.amount 等)
   - 建立時機:實作 OTel instrumentation 時
   - 架構指引:已提供 span attributes 範例 (多處)

4. **`docs/exception-hierarchy.md`**
   - 內容:自訂 Exception 類別階層圖
   - 建立時機:實作錯誤處理時
   - 架構指引:已提供 Level 2 錯誤分類 (business/external/system)

**Nice-to-Have Gaps (可選缺口): 3 個**

以下項目為可選優化,不影響 Phase 1 實作:

1. **Grafana Dashboard 預設配置**
   - 狀態:可在部署 Grafana 時手動建立
   - 優先級:Phase 2

2. **Locust 壓測腳本細節**
   - 狀態:已規劃 `tests/performance/locustfile.py`,可在效能測試時補充
   - 優先級:Phase 2

3. **Renovate Bot 配置檔**
   - 狀態:已文件化依賴審計流程,可選擇性建立 `renovate.json`
   - 優先級:Phase 2

### Validation Issues Addressed

**透過 Advanced Elicitation 解決的問題:**

在架構驗證過程中,發現 3 個小建議並透過 **Expert Panel Review** 和 **Architecture Decision Records** 方法成功補充:

1. **API Gateway 角色澄清** ✅
   - 原始狀態:提及但細節不足
   - 解決方案:調整為 Phase 1 必備,新增 Spring Cloud Gateway 3.1.x 完整配置
   - 影響:前端簡化為單一 endpoint,JWT 驗證集中化,新增 JWT Secret 輪換策略

2. **Material-UI 版本規格** ✅
   - 原始狀態:未明確版本
   - 解決方案:指定 @mui/material ^5.15.0,新增 Bundle 優化策略
   - 影響:新增 Code Splitting, Icons 按需載入, Core Web Vitals 效能目標

3. **Nginx 配置細節** ✅
   - 原始狀態:基本配置,缺少安全強化
   - 解決方案:新增完整安全標頭 (CSP, Referrer-Policy, Permissions-Policy), WebSocket 超時優化
   - 影響:API proxy 啟用 (Phase 1 必備), Request ID 傳遞,生產級安全防護

**所有問題已解決,無遺留議題。**

### Architecture Completeness Checklist

**✅ Requirements Analysis (需求分析) - 4/4 完成**

- [x] 專案情境完整分析 (銀行業務 + 可觀測性展示)
- [x] 規模與複雜度評估 (高複雜度,10 微服務,3 語言,25+ 元件)
- [x] 技術約束識別 (Quarkus 必須手動 SDK, SAGA 同步處理)
- [x] 跨領域關注點映射 (OpenTelemetry, JWT, 錯誤處理, 資料庫存取)

**✅ Architectural Decisions (架構決策) - 9/9 完成**

- [x] 關鍵決策文件化並標註版本 (Flyway 10.x, Spring Cloud Gateway 3.1.x, MUI 5.15.0, Nginx 1.25)
- [x] 技術棧完整規格化 (Java SpringBoot/Quarkus, Python FastAPI, React + TypeScript)
- [x] 整合模式定義 (HTTP 同步, Kafka 異步, WebSocket 推播)
- [x] 效能考量處理 (Bundle 優化, Gateway 延遲評估, OTel 採樣策略)
- [x] 安全性決策 (JWT Secret 輪換, Nginx 安全標頭, CORS 集中管理)
- [x] 資料庫策略 (Shared Schema, Flyway 遷移, 多幣別設計)
- [x] 前端架構 (Context API, React Router v6, Socket.io-client)
- [x] 可觀測性架構 (OTel Collector, Grafana Stack, 手動 SDK)
- [x] 部署策略 (K8s Helm Charts, 4 Namespaces, 一鍵部署)

**✅ Implementation Patterns (實作模式) - 5/5 完成**

- [x] 命名規範建立 (Database snake_case, API camelCase, Code 語言慣例)
- [x] 結構模式定義 (Java MVC, Python routers/services, React pages/components)
- [x] 通訊模式規格化 (HTTP traceparent header, Kafka trace propagation, WebSocket _trace field)
- [x] 流程模式文件化 (SAGA 補償, 錯誤處理, JWT 輪換, Bundle 優化)
- [x] 優化策略明確 (Code Splitting, Icon 按需載入, Nginx 安全強化, Core Web Vitals)

**✅ Project Structure (專案結構) - 4/4 完成**

- [x] 完整目錄結構定義 (10 微服務 + Frontend + Infrastructure + Chaos + Tests + Scripts)
- [x] 元件邊界建立 (API Gateway 入口, K8s Namespace 隔離, 服務間 HTTP/Kafka)
- [x] 整合點映射 (Gateway 路由, Kafka topics, WebSocket endpoint, Database access)
- [x] 需求至結構映射完成 (9 Epics → 具體檔案路徑)

### Architecture Readiness Assessment

**Overall Status (整體狀態):** ✅ **READY FOR IMPLEMENTATION**

**Confidence Level (信心等級):** **高 (High)**

**評估理由:**
- 所有關鍵決策已完成並文件化
- 3 個架構澄清項目已透過 Advanced Elicitation 補充完善
- 無阻擋實作的關鍵缺口
- 實作模式完整,AI agents 可一致性執行
- 專案結構具體且可直接使用

**Key Strengths (關鍵優勢):**

1. **完整的衝突預防機制**
   - 25+ 個潛在 AI agent 衝突點已預先識別並解決
   - 跨語言命名規範明確 (JSON camelCase, Database snake_case)
   - Code Review Checklist 可執行

2. **具體的專案結構**
   - 非通用模板,直接可用的完整目錄樹
   - 每個微服務包含 controller/service/repository/model/exception/config/telemetry
   - Frontend 包含 pages/components/contexts/hooks/services/telemetry

3. **跨語言一致性保證**
   - Java, Python, TypeScript 命名與格式全部對齊
   - Pydantic aliases 明確定義 snake_case → camelCase 轉換
   - 錯誤格式統一 (ERR_001 + JSON structure)

4. **生產級配置**
   - API Gateway 集中驗證與路由 (Spring Cloud Gateway 3.1.x)
   - Nginx 完整安全標頭 (CSP, Referrer-Policy, Permissions-Policy)
   - Bundle 優化策略 (Code Splitting, manualChunks, 效能目標)
   - JWT Secret 輪換策略 (90 天週期, 24 小時 Grace Period)

5. **完整的 Trace 路徑**
   - Frontend (Browser SDK) → Nginx (traceparent) → API Gateway (注入) → 微服務 (propagate) → Kafka (headers) → Notification Service → WebSocket (_trace) → Frontend (display)
   - 所有層級都有明確的 trace context 傳遞機制

**Areas for Future Enhancement (未來增強領域):**

Phase 2 可選優化項目 (不影響 Phase 1 實作):

1. **效能優化**
   - Brotli 壓縮 (優於 Gzip)
   - HTTP/2 啟用
   - Redis 快取層 (帳戶餘額, 匯率)

2. **安全強化**
   - External Secrets Operator (替代 K8s Secret)
   - WebSocket Origin 驗證強制啟用
   - Rate Limiting 細粒度配置

3. **可觀測性增強**
   - 完整的 Grafana Dashboard 模板
   - 預設 Alert Rules
   - 業務 Metrics (Phase 1 僅應用層級)

4. **SAGA 恢復邏輯**
   - Pod 重啟後 SAGA 狀態恢復
   - 掃描 `IN_PROGRESS` 狀態並繼續執行

5. **架構演進**
   - Separate Schemas 評估 (從 Shared Schema 遷移)
   - Service Mesh 評估 (Istio/Linkerd)
   - API Gateway 進階功能 (Circuit Breaker, Retry)

### Implementation Handoff

**AI Agent 實作指南:**

1. **嚴格遵循架構決策**
   - 所有技術版本必須符合文件規格
   - 不得任意變更命名規範或結構模式
   - 所有架構問題必須參考本文件

2. **使用實作模式確保一致性**
   - JSON 欄位一律使用 camelCase
   - Database 命名一律使用 snake_case
   - 錯誤回應必須包含 `code`, `type`, `category`, `traceId`
   - 測試檔案必須放置於分離目錄

3. **尊重專案結構與邊界**
   - 不得跨服務直接寫入資料庫 (參考 `docs/database-access-rules.md`)
   - API 必須透過 Gateway 存取
   - Trace context 必須在所有層級傳遞

4. **Code Review 必檢項目**
   - [ ] JSON 欄位是否使用 camelCase?
   - [ ] 測試檔案是否放置於正確位置?
   - [ ] 檔案命名是否符合語言慣例?
   - [ ] 錯誤回應是否包含 `code`, `type`, `category`, `traceId`?
   - [ ] 日期格式是否使用 ISO 8601?
   - [ ] Database 命名是否使用 snake_case?
   - [ ] Span attributes 是否包含業務語義?
   - [ ] Trace context 是否正確傳遞?

**First Implementation Steps (首要實作步驟):**

**階段 1: 環境準備**
```bash
# 1. 建立專案目錄結構
mkdir -p services/{api-gateway,user-service,account-service,transaction-service,deposit-withdrawal-service,transfer-service,currency-exchange-service,exchange-rate-service,notification-service}
mkdir -p frontend infrastructure/{k8s,helm,docker-compose} chaos-scenarios tests scripts docs

# 2. 初始化 Git repository
git init
git add .
git commit -m "Initial commit: Project structure"

# 3. 建立 K8s namespaces
kubectl apply -f infrastructure/k8s/namespaces/
```

**階段 2: 基礎設施部署**
```bash
# 4. 部署 PostgreSQL
kubectl apply -f infrastructure/k8s/databases/

# 5. 執行 Flyway schema 初始化
# (User Service, Account Service, Transaction Service, Transfer Service 各自執行)

# 6. 部署 Kafka
kubectl apply -f infrastructure/k8s/kafka/

# 7. 部署 Grafana Stack
kubectl apply -f infrastructure/k8s/observability/
```

**階段 3: 微服務實作順序**
```
1. User Service (優先,提供 JWT 認證)
   └─ 實作 /api/v1/auth/login
   └─ 實作 JWT 簽發邏輯
   └─ 建立 users 表 (V1__create_users_table.sql)

2. API Gateway (統一入口)
   └─ 配置 Spring Cloud Gateway
   └─ 實作 JWT 驗證 filter
   └─ 配置所有微服務路由

3. Account Service (Quarkus, 手動 SDK)
   └─ 實作 /api/v1/accounts
   └─ 建立 accounts 表 (V2__create_accounts_table.sql)
   └─ 手動實作 OpenTelemetry instrumentation

4. Exchange Rate Service (Python, mock 外部 API)
   └─ 實作 /api/v1/rates/{from}/{to}
   └─ 提供模擬匯率資料

5. Transaction Service
   └─ 實作 /api/v1/transactions
   └─ 建立 transactions 表 (V3__create_transactions_table.sql)

6. Deposit/Withdrawal Service
   └─ 實作 /api/v1/deposits, /api/v1/withdrawals

7. Currency Exchange Service (Quarkus)
   └─ 實作 /api/v1/exchanges

8. Transfer Service (Python, SAGA 編排器)
   └─ 實作 SAGA 6 步驟流程
   └─ 實作補償邏輯
   └─ 建立 saga_executions 表 (V4__create_saga_executions_table.sql)

9. Notification Service (Kafka + WebSocket)
   └─ 實作 Kafka consumer
   └─ 實作 WebSocket server

10. Frontend (React + MUI)
    └─ 實作 pages, components, contexts
    └─ 整合 OpenTelemetry Browser SDK
    └─ 整合 Socket.io-client
```

**階段 4: 整合測試與 Chaos**
```bash
# 執行整合測試
pytest tests/integration/

# 部署 Chaos Mesh
kubectl apply -f infrastructure/k8s/chaos-mesh/

# 執行 Chaos 場景
kubectl apply -f chaos-scenarios/network-delay-exchange-rate.yaml
```

**優先實作檔案清單:**

1. `services/user-service/src/main/resources/db/migration/V1__create_users_table.sql`
2. `services/user-service/src/main/java/com/bank/user/service/JwtService.java`
3. `services/api-gateway/src/main/resources/application.yml` (Gateway 路由配置)
4. `services/account-service/src/main/resources/db/migration/V2__create_accounts_table.sql`
5. `services/transfer-service/app/services/transfer_orchestrator.py` (SAGA 核心)
6. `frontend/src/telemetry/tracer.ts` (Browser SDK)
7. `infrastructure/k8s/namespaces/namespace-services.yaml`
8. `docs/database-access-rules.md` (服務-表格存取權限)

---

## Next Steps

架構決策文件已全部完成,系統已就緒進入實作階段。

**建議下一步:**
1. 執行專案初始化 (建立目錄結構)
2. 開始實作 User Service (提供 JWT 認證)
3. 實作 API Gateway (統一入口)
4. 逐步實作其他微服務,遵循架構文件規範

**所有架構決策已鎖定,實作過程中如有疑問,請參考本文件對應章節。**

