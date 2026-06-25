# Alert 設計規範

> 實作落地：`grafana/alerting/slo-alerts.yaml`（Grafana-managed alert rules provisioning，
> 由 docker-compose 掛載至 `/etc/grafana/provisioning/alerting`）。
> SLO 面板：`grafana/dashboards/slo.json`。

## 指標前提（務必先讀）

所有業務告警都建立在 **Alloy spanmetrics** 上，錯誤維度是 **OTel span status**，不是 HTTP 狀態碼：

- 服務標籤是 `service_name`（非 `service`）。轉帳/存款/提款都在 **`teller-service`**，沒有獨立的 `transfer-service`。
- 錯誤判定一律用 `status_code!="STATUS_CODE_ERROR"`（值域只有 `STATUS_CODE_ERROR` / `STATUS_CODE_OK` / `STATUS_CODE_UNSET`）。**spanmetrics 沒有 `5xx` 維度，任何 `status_code=~"5.."` 都 match 不到。**
- 只計入伺服器端 span：`span_kind="SPAN_KIND_SERVER"`。
- 路由含版本前綴，例如 `/api/v1/auth/login`、`/api/v1/accounts/{accountId}/balance`。

基礎設施告警建立在 **Micrometer** 指標（`hikaricp_*` / `jvm_*` / `tomcat_*`），分組標籤為 `application`。

---

## 告警分層

```
Critical  → 立即影響用戶，需立刻處理（PagerDuty / 電話）
Warning   → Error Budget 加速消耗，需當班處理（Slack）
Info      → 趨勢異常，需關注（Dashboard annotation）
```

---

## SLO 與 Error Budget 對照

| SLO | service_name / http_route | 目標 | budget (1−SLO) | P99 SLO |
|-----|---------------------------|------|----------------|---------|
| 轉帳 | teller-service `/api/v1/transfers` | 99.5% | 0.005 | 3000ms |
| 存款 | teller-service `/api/v1/deposits` | 99.5% | 0.005 | 2000ms |
| 提款 | teller-service `/api/v1/withdrawals` | 99.5% | 0.005 | 2000ms |
| 換匯 | exchange-service `/api/v1/exchanges` | 99.0% | 0.01 | 5000ms |
| 登入 | user-service `/api/v1/auth/login` | 99.9% | 0.001 | 1000ms |
| 帳戶查詢 | account-service `/api/v1/accounts/{accountId}/balance` | 99.9% | 0.001 | 500ms |

---

## 業務層告警 — Error Budget Burn Rate（MWMB）

採 Google SRE 多窗口多燃燒率：**雙窗口同時超標才告警**，短窗口讓告警快速消除、長窗口避免瞬時噪音。

| 等級 | burn rate | 窗口（AND） | 意義 |
|------|-----------|-------------|------|
| Critical | > 14.4x | 1h **且** 5m | 約 1h 燒掉 2% 月度 budget |
| Warning | > 6x | 6h **且** 30m | budget 消耗加速 |

實作上把雙窗口 AND 直接寫進單一 PromQL，用 `and` operator；不燒時回空向量，搭配 `noDataState: OK` 視為健康。以轉帳 Critical 為例（其餘 SLO 改 `service_name` / `http_route` / budget 即可）：

```promql
(
  (1 -
    sum(rate(litebank_calls_total{service_name="teller-service",http_route="/api/v1/transfers",span_kind="SPAN_KIND_SERVER",status_code!="STATUS_CODE_ERROR"}[1h]))
    /
    sum(rate(litebank_calls_total{service_name="teller-service",http_route="/api/v1/transfers",span_kind="SPAN_KIND_SERVER"}[1h]))
  ) / 0.005 > 14.4
)
and
(
  (1 -
    sum(rate(litebank_calls_total{service_name="teller-service",http_route="/api/v1/transfers",span_kind="SPAN_KIND_SERVER",status_code!="STATUS_CODE_ERROR"}[5m]))
    /
    sum(rate(litebank_calls_total{service_name="teller-service",http_route="/api/v1/transfers",span_kind="SPAN_KIND_SERVER"}[5m]))
  ) / 0.005 > 14.4
)
```

> Warning 版把窗口換成 `[6h]` / `[30m]`、倍率換成 `6`。

6 個 SLO × 2 等級 = 12 條 burn rate 規則。

---

## 業務層告警 — P99 延遲

| 等級 | 閾值 | 持續時間 |
|------|------|---------|
| Warning | > SLO 線 | 5m |
| Critical | > 2 × SLO | 2m |

```promql
histogram_quantile(0.99,
  sum(rate(litebank_duration_milliseconds_bucket{service_name="teller-service",http_route="/api/v1/transfers",span_kind="SPAN_KIND_SERVER"}[5m])) by (le)
) > 3000
```

6 個 SLO × 2 等級 = 12 條延遲規則。

---

## 基礎設施層告警

### INF-001：transaction-service 錯誤率（唯一寫帳服務）

```promql
sum(rate(litebank_calls_total{service_name="transaction-service",span_kind="SPAN_KIND_SERVER",status_code="STATUS_CODE_ERROR"}[5m]))
/
sum(rate(litebank_calls_total{service_name="transaction-service",span_kind="SPAN_KIND_SERVER"}[5m]))
> 0.01
```

| 等級 | 閾值 | 持續時間 |
|------|------|---------|
| Warning | > 1% | 2m |
| Critical | > 5% | 2m |

### INF-002：DB 連線池排隊（Micrometer）

```promql
hikaricp_connections_pending > 2
```

| 等級 | 閾值 | 持續時間 |
|------|------|---------|
| Warning | pending > 2 | 3m |
| Critical | pending > 10 | 1m |

### INF-003：JVM Heap 壓力（Micrometer）

```promql
sum by (application) (jvm_memory_used_bytes{area="heap"})
/
sum by (application) (jvm_memory_max_bytes{area="heap"})
* 100 > 85
```

| 等級 | 閾值 | 持續時間 |
|------|------|---------|
| Warning | > 85% | 5m |
| Critical | > 95% | 2m |

### INF-004：Tomcat 執行緒耗盡（Micrometer）

```promql
tomcat_threads_busy_threads / tomcat_threads_config_max_threads > 0.8
```

| 等級 | 閾值 | 持續時間 |
|------|------|---------|
| Warning | > 80% | 3m |
| Critical | > 90% | 1m |

### INF-005：服務沉默（spanmetrics 完全消失）

用 `absent()` 偵測服務流量歸零；此規則 `noDataState: Alerting`（與 budget 類規則的 `OK` 相反），確保服務完全沉默時仍會告警。

```promql
absent(litebank_calls_total{service_name="api-gateway",span_kind="SPAN_KIND_SERVER"})
```

涵蓋 `api-gateway`、`teller-service`、`transaction-service` 三個關鍵服務。

---

## 告警路由設計（尚未 provision，目前用 Grafana 預設）

```
告警 → Grafana Alerting
         ├── Critical → Webhook → AI SRE Agent（自動診斷）
         │                      + PagerDuty（人工介入）
         └── Warning  → Webhook → AI SRE Agent（自動診斷）
                                + Slack #sre-alerts
```

AI SRE Agent 收到 Webhook 後：
1. 解析告警 labels（`service`/`service_name`、`severity`、`layer`、`slo`、`alertname`）
2. 自動執行三層調查（Metrics → Logs → Traces）
3. 產出診斷報告
4. 將報告貼回 Grafana Incident 或 Slack thread

> contact point 與 notification policy 尚未寫進 provisioning，待路由實作時補上。
> 規則已帶 `severity` / `layer` / `slo` labels，路由 matcher 可直接使用。

---

## 告警品質原則

- **Symptom 與 cause 都告警，嚴重度不同**：`burn rate`（轉帳失敗）是 symptom，`INF-002`（連線池耗盡）是 cause。
- **多窗口雙重確認**：burn rate 用 short + long window AND，避免瞬間噪音。
- **noData 語意明確**：budget／延遲／資源類規則 noData = OK（健康），服務沉默另由 INF-005 `absent()` 以 noData = Alerting 把關。
- **告警必須 actionable**：每條告警都對應到 Runbook 或 AI SRE 的調查邏輯。
```