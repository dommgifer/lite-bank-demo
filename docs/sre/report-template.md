# AI SRE 診斷報告規範

## 報告觸發條件

每次 Warning 或 Critical 告警觸發時自動產生。報告不替代人工判斷，而是幫助 SRE 在最短時間內掌握問題全貌。

---

## 報告結構

```
1. 告警摘要       ← 30 秒能看懂的情況
2. 影響範圍評估   ← 哪些用戶 / 操作受影響
3. 指標快照       ← 數字說話
4. 日誌分析       ← 錯誤模式，不是 raw log
5. Trace 分析     ← 找瓶頸 span
6. 根因假設       ← AI 推論，附信心分數
7. 建議行動       ← 立即 / 短期 / 長期
```

---

## 報告範本

```markdown
# AI SRE 診斷報告

**告警名稱**：{alertname}
**觸發時間**：{fired_at} UTC+8
**嚴重度**：{severity}（Critical / Warning）
**受影響服務**：{service}
**報告產出時間**：{generated_at}（告警後 {elapsed} 秒）

---

## 告警摘要

{一句話描述問題，例如：transfer-service P99 延遲在過去 10 分鐘從 450ms 升至 3840ms，觸發 SLO 違規}

---

## 影響範圍

| 維度 | 數據 |
|------|------|
| 受影響操作 | 轉帳（/api/transfer） |
| 當前流量 | {current_rps} req/s |
| 估算受影響用戶 | {affected_requests} 筆請求（過去 10 分鐘） |
| Error Budget 消耗 | 本月已消耗 {budget_consumed}%（剩餘 {budget_remaining} 分鐘） |

---

## 指標快照

### 業務指標（Spanmetrics）

| 指標 | 當前值 | 基線（過去 7 天同時段） | 狀態 |
|------|--------|----------------------|------|
| P50 延遲 | {p50}ms | {p50_baseline}ms | {status} |
| P99 延遲 | {p99}ms | {p99_baseline}ms | {status} |
| 成功率 | {success_rate}% | {success_rate_baseline}% | {status} |
| 吞吐量 | {rps} rps | {rps_baseline} rps | {status} |

### 基礎設施指標（Micrometer）

| 指標 | 值 | 閾值 | 狀態 |
|------|-----|------|------|
| HikariCP pending | {hikari_pending} | > 2 = Warning | {status} |
| HikariCP active / max | {hikari_active}/{hikari_max} | | {status} |
| Heap 使用率 | {heap_pct}% | > 85% = Warning | {status} |
| Tomcat 執行緒使用率 | {tomcat_pct}% | > 80% = Warning | {status} |

---

## 日誌分析

**查詢範圍**：{service}，過去 {window} 分鐘，ERROR 等級以上

### 錯誤模式（Top 5）

| 錯誤訊息模式 | 出現次數 | 首次出現 | 最後出現 |
|------------|---------|---------|---------|
| {error_pattern_1} | {count} | {first_seen} | {last_seen} |
| {error_pattern_2} | {count} | {first_seen} | {last_seen} |

### 關聯服務日誌

{如果上游或下游服務有同步出現錯誤，列出摘要}

---

## Trace 分析

**查詢範圍**：{service}，過去 {window} 分鐘，P99 以上的慢 Trace

### 最慢 Trace Span 分解

```
TraceID: {trace_id}  總耗時: {total_duration}ms

{service} → {operation}               {self_duration}ms
  └── {downstream_service} → {op}     {duration}ms  ← 瓶頸
        └── db.query: {sql_summary}   {duration}ms
```

### 錯誤 Trace 分佈

| Span 名稱 | 錯誤率 | 平均耗時 |
|---------|------|---------|
| {span_1} | {err_rate}% | {avg_ms}ms |

---

## 根因假設

AI 基於以上數據推論，信心分數供參考：

| 假設 | 信心 | 支持證據 |
|------|------|---------|
| {hypothesis_1} | {confidence}% | {evidence} |
| {hypothesis_2} | {confidence}% | {evidence} |

---

## 建議行動

### 立即（< 5 分鐘）
- [ ] {immediate_action_1}
- [ ] {immediate_action_2}

### 短期（本班）
- [ ] {short_term_action_1}

### 長期（下次 Sprint）
- [ ] {long_term_action_1}

---

## 附錄：查詢連結

- [Grafana Dashboard]({grafana_dashboard_url})
- [Loki 日誌查詢]({loki_explore_url})
- [Tempo Trace 詳情]({tempo_trace_url})
- [Prometheus 指標]({prometheus_url})
```

---

## 報告品質原則

- **不貼 raw log**：只貼錯誤模式摘要和出現次數
- **不貼原始 time series**：只貼特徵值（當前值 vs 基線）
- **根因假設附信心分數**：AI 不是神，讓 SRE 自行判斷
- **行動項目要具體**：不寫「檢查問題」，要寫「檢查 transaction-service HikariCP pending count」
- **報告要在 60 秒內產出**：查詢太慢的工具要做 timeout 保護
