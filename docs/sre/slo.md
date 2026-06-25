# Lite Bank SLO 定義

## 設計原則

SLO → Error Budget → Alert 的推導鏈：
- SLO 定義「什麼叫健康」
- Error Budget 定義「可以壞多久」
- Alert 在 budget 燒太快時才響，避免噪音

---

## 業務層 SLO

這些 SLO 直接對應用戶體驗，是最重要的告警依據。

| 業務操作 | 服務 | 可用性 SLO | P99 延遲 SLO | 月 Error Budget |
|---------|------|-----------|-------------|----------------|
| 轉帳 | transfer-service | 99.5% | 3000ms | 216 分鐘 |
| 存款 / 提款 | teller-service | 99.5% | 2000ms | 216 分鐘 |
| 換匯 | exchange-service | 99.0% | 5000ms | 432 分鐘 |
| 登入 | user-service | 99.9% | 1000ms | 43 分鐘 |
| 帳戶查詢 | account-service | 99.9% | 500ms | 43 分鐘 |

### Error Budget 計算方式

```
月 Error Budget（分鐘）= 43,200 × (1 - SLO)

範例：
  轉帳 99.5% → 43,200 × 0.005 = 216 分鐘
  登入 99.9% → 43,200 × 0.001 = 43 分鐘
```

---

## 基礎設施層 SLO

支撐業務層的關鍵內部服務，SLO 通常比業務層嚴一個數量級。

| 服務 | 可用性 SLO | P99 延遲 SLO | 說明 |
|------|-----------|-------------|------|
| transaction-service | 99.9% | 500ms | 所有餘額變更唯一入口 |
| account-service | 99.9% | 300ms | 被多個 Coordination 服務依賴 |
| api-gateway | 99.9% | 200ms（自身 overhead） | 所有外部流量入口 |
| exchange-rate-service | 99.5% | 200ms | 換匯依賴，stateless |

---

## Metrics 對應

### 可用性（Success Rate）

```promql
# 轉帳成功率（5xx 視為失敗）
sum(rate(litebank_calls_total{service="transfer-service", status_code!~"5.."}[5m]))
/
sum(rate(litebank_calls_total{service="transfer-service"}[5m]))
```

### P99 延遲

```promql
# 轉帳 P99 latency
histogram_quantile(0.99,
  sum(rate(litebank_duration_milliseconds_bucket{service="transfer-service"}[5m])) by (le)
)
```

### HikariCP 連線池（Micrometer）

```promql
# DB 連線池等待數（> 0 代表開始排隊）
hikaricp_connections_pending{application="transaction-service"}

# 連線池使用率
hikaricp_connections_active{application="transaction-service"}
/
hikaricp_connections_max{application="transaction-service"}
```

### JVM 記憶體壓力（Micrometer）

```promql
# Heap 使用率
jvm_memory_used_bytes{area="heap", application="transfer-service"}
/
jvm_memory_max_bytes{area="heap", application="transfer-service"}
```

---

## SLO Review 週期

| 週期 | 行動 |
|------|------|
| 每週 | 檢視 Error Budget 消耗速率 |
| 每月 | 統計各服務 SLO 達成率，調整閾值 |
| 每季 | 根據業務成長重新評估 SLO 數字 |
