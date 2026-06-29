# AI SRE 工具層設計（Query Registry + 薄封裝）

> 決策來源：三方 AI 辯論（Claude / Codex / Gemini）一致結論 —
> **混合架構，方案 B（語意化封裝）為主路徑，Grafana MCP 為受限 fallback**。
> 理由：可重現、預先降維、硬約束區間/資料量、60 秒預算下減少 round-trip。

## 設計目標

| 目標 | 怎麼達成 |
|------|---------|
| 可重現 | PromQL/LogQL/TraceQL 固化在 registry，LLM 只傳結構化參數 |
| 不被 raw data 淹沒 | 封裝層先聚合成特徵值（p50/p99/success_rate/rps…），不回原始 time series |
| 區間不爆炸 | 每個 query 在程式裡 clamp `range`、限制 series 數與回傳筆數 |
| 不浪費 token 試錯 | 參數用 enum 驗證，失敗回**引導式錯誤**（給合法值），LLM 一次修正 |
| 60 秒產報告 | 主路徑 = 固定 3~5 次毫秒級呼叫；timeout 8s 保護 |
| 處理未知告警 | 保留 Grafana MCP 當逃生門（受 policy 限制） |

---

## 架構與資料流

```
Grafana Alert (webhook / 手動 /ai-sre)
        │  alertname, service_name, severity, layer, slo, http_route
        ▼
  AI SRE Agent (Claude Code skill)
        │  只能呼叫語意工具，傳結構化參數
        ▼
  sre_query.py  ◄── queries.yaml (Query Registry)
        │  1. 查 registry 取模板
        │  2. 驗證/clamp 參數（引導式錯誤）
        │  3. 套參數產出 PromQL/LogQL/TraceQL
        │  4. 經 Grafana datasource proxy 執行（單一 endpoint + 單一 token）
        │  5. 後處理：聚合成特徵值 + baseline 比對
        ▼
  結構化 JSON（特徵值，非 raw data）
        │
        ▼
  LLM 推論根因 → 按 report-template.md 產報告
        │
        └── fallback：特徵值不足以定位 → 受限 Grafana MCP 深挖
```

**連線方式**：見下節「資料源分流」。數值型查詢走 Grafana datasource proxy
（`POST /api/datasources/proxy/uid/<uid>/api/v1/query`，單一 endpoint + 單一 token），
探索型查詢（找模式 / 找瓶頸）走 Grafana MCP。

---

## 資料源分流（重要設計原則）

封裝層底下**不是單一 transport**，而是按「工作性質」分兩條路。
（決策來源：與 ChatGPT「全走 MCP」方案比較後的結論——全走 MCP 多一跳、輸出形狀失控、
把確定性主路徑建在通用層上自相矛盾；但 Sift / pattern 聚合自己重做又太費工，故分流。）

### 判準（一句話）

- **「我已經知道要量什麼，只要它快又準」→ 直打 API**（metrics / RED / baseline / 資源）
- **「我不知道答案長怎樣，要它幫我找模式 / 找異常」→ 走 MCP**（Sift 慢 trace、log 模式聚合、Asserts）

### 兩條路各自的理由

| | 數值型 → **直打 API** | 探索型 → **走 MCP** |
|---|---|---|
| 適用 | RED 指標、基礎設施、baseline 比對 | log 模式聚類、慢 trace 分解、異常偵測 |
| 為什麼 | 要精準控制 range/step/max_series、輸出形狀我定、可審計、少一跳 | Sift / `find_error_pattern_logs` 已內建演算法，自己用 LogQL/TraceQL 重做等於重造輪子 |
| 工具 | `sre_query.py` → datasource proxy | `find_error_pattern_logs` / `find_slow_requests` / `get_assertions` |

> 結論：**數值主幹自己控（B），模式探索借 MCP 的現成腦力**。不是 fallback 才用 MCP——
> Step 4/5 本來就走 MCP，這是主流程的一部分；MCP 另外仍兼任「未知告警深挖」的逃生門。

### 職責邊界：誰呼叫什麼（重要）

**`sre_query.py` 絕不呼叫 MCP**。理由：MCP 協定是設計給「LLM agent ↔ 工具」的，
讓一支程式去呼叫 MCP 是方向反了（還要在腳本內塞 MCP client）。因此切分為：

| 角色 | 負責 | 不負責 |
|------|------|--------|
| `sre_query.py`（封裝層） | 數值/API 路徑：Step 0/2/3/6，直打 datasource proxy | ❌ 不碰 MCP |
| **LLM agent**（skill.md 指示） | Step 4/5 直接呼叫 MCP 工具（Sift / pattern），整合各步結論寫報告 | ❌ 不自己組 PromQL |

也就是：封裝工具 = 純 API 數值層；MCP = LLM 自己用的探索層。兩者在 LLM 這一層匯合。
skill.md 因此要寫成「數值步驟呼叫 `sre_query.py`，探索步驟直接呼叫 MCP 工具」。

### 調查邊界：查到「分流深度」就交棒

工具的責任是**「定位根因在哪一層」，不是「修好那一層」**。一路往下分流，但每一層都只查到
能判斷「問題在這層還是更下層」即可，不做該層的深度除錯：

```
業務(spanmetrics) → app 資源(Micrometer) → 平台(pod/node) → [更下層]
   單點/連鎖?          流量/劣化?           app/平台?         交棒
```

- **平台層（Step 3.5）**只查到「app 問題 vs 平台問題」：pod 重啟/OOMKilled、CPU throttle、node pressure。
- 一旦判定根因在平台層（node 為什麼記憶體不足、要不要 cordon/drain、kubelet 設定、硬體）
  → **報告標明「根因在平台層」並交棒給平台團隊 runbook，AI SRE 不往下鑽。**
- 這與「Step 0 掃依賴是為了分流、不是去修依賴」是同一個原則，只是再往下一層。

#### 控制平面 / etcd：明確 out-of-scope

`apiserver_*` / `etcd_*` 指標雖然在 Prometheus 抓得到，**仍不列為例行調查步驟**：

1. **不在資料路徑上**：跑著的 pod 處理交易請求不碰 apiserver/etcd；etcd 變慢不會讓轉帳變慢
   （app 的資料庫是 PostgreSQL，不是 etcd）。本 agent 處理的是「請求服務層」告警，控制平面屬
   「編排/變更層」，幾乎永遠不是這類告警的根因。
2. **在交棒線更下面**：比 node 更深，是叢集管理員的絕對領域，app 團隊 AI SRE 零修復權限。

**唯一例外用「廣度」偵測，不靠查 etcd**：控制平面問題若傷到 app，症狀是「pod 掛了排不回來 /
大範圍服務同時異常」——這個 `platform_health`（pod Pending/重啟）與下面的廣度規則已能捕捉。

#### 廣度判斷規則（Step 0 觸發）

若 `service_health` 發現**大範圍多服務同時異常**（或多條 `INF-005 absent()` 同時響），
判定「**疑似叢集 / 平台級事件**」→ 直接標記升級平台團隊，**跳過逐服務深查與三層調查**。

```
單一/少數服務異常 → 正常走 app/平台分流（Step 0~3.5 + MCP）
大範圍同時異常     → 廣度即訊號 → 判定叢集級 → 升級平台團隊，不查 etcd 細節
```

> 「這是不是控制平面問題」靠**症狀廣度**判斷，不靠 etcd 指標。廣度一觸發，結論就是升級，
> 而非 debug etcd fsync 延遲。

---

## 目錄結構

```
.claude/skills/ai-sre/
├── skill.md              # 改寫：工具段指向語意工具 + fallback 規範
├── queries.yaml          # Query Registry（本設計核心）
├── sre_query.py          # 薄封裝 CLI：驗證→clamp→執行→聚合→JSON
└── lib/
    └── grafana.py        # datasource proxy client + token 讀取
```

執行方式（agent 透過 Bash 呼叫）：
```bash
python .claude/skills/ai-sre/sre_query.py \
  --query service_metrics --service teller-service \
  --http-route /api/v1/transfers --range 30m --baseline 1d
```

---

## Query Registry 結構（queries.yaml）

```yaml
defaults:
  timeout_s: 8                 # 硬性逾時，保護 60 秒預算
  max_range: 3h                # 任何 query 的區間上限
  baseline_offset: 1d          # ⚠️ 預設改 1d（見下方「基線可行性」）
  max_series: 50               # 回傳 series 上限，超過則報錯要求縮窗
  proxy: true                  # 走 Grafana datasource proxy

# 合法值來源（enum 驗證用；可定期由腳本從 Prometheus 自動更新）
enums:
  service:
    - api-gateway
    - user-service
    - account-service
    - transaction-service
    - teller-service
    - exchange-service
    - exchange-rate-service
    - analytics-processor-service
    - analytics-query-service
    - notification-service

queries:

  # ── Step 2：業務指標（spanmetrics）──────────────────────
  service_metrics:
    description: 業務層 RED 特徵值 + 基線比對
    datasource: prometheus
    params:
      service:    {required: true, enum: service}
      http_route: {required: false}            # 不給則整服務聚合
      range:      {default: 30m, max: 3h}
    label_filters:
      base: 'service_name="${service}",span_kind="SPAN_KIND_SERVER"'
      route: '${http_route ? `,http_route="${http_route}"` : ""}'
    metrics:
      success_rate:
        unit: ratio
        expr: |
          sum(rate(litebank_calls_total{${base}${route},status_code!="STATUS_CODE_ERROR"}[${range}]))
          / sum(rate(litebank_calls_total{${base}${route}}[${range}]))
      rps:
        unit: req/s
        expr: sum(rate(litebank_calls_total{${base}${route}}[${range}]))
      p50:
        unit: ms
        expr: histogram_quantile(0.50, sum(rate(litebank_duration_milliseconds_bucket{${base}${route}}[${range}])) by (le))
      p99:
        unit: ms
        expr: histogram_quantile(0.99, sum(rate(litebank_duration_milliseconds_bucket{${base}${route}}[${range}])) by (le))
    baseline: true            # 每個 metric 自動再跑一次 (offset baseline_offset)
    output: scalar_per_metric # {current, baseline, delta_pct, trend}

  # ── Step 3：基礎設施指標（Micrometer）──────────────────
  infra_metrics:
    description: HikariCP / JVM heap / Tomcat threads
    datasource: prometheus
    params:
      service: {required: true, enum: service}  # 對映 application 標籤
      range:   {default: 15m, max: 1h}
    metrics:
      hikaricp_pending:
        expr: max_over_time(hikaricp_connections_pending{application="${service}"}[${range}])
      hikaricp_util:
        unit: ratio
        expr: |
          max_over_time(hikaricp_connections_active{application="${service}"}[${range}])
          / max(hikaricp_connections_max{application="${service}"})
      heap_util:
        unit: ratio
        expr: |
          max_over_time(jvm_memory_used_bytes{area="heap",application="${service}"}[${range}])
          / max(jvm_memory_max_bytes{area="heap",application="${service}"})
      tomcat_util:
        unit: ratio
        expr: |
          max_over_time(tomcat_threads_busy_threads{application="${service}"}[${range}])
          / max(tomcat_threads_config_max_threads{application="${service}"})
    baseline: false           # 資源類看絕對值對閾值，不需基線
    output: scalar_per_metric

  # ── Step 3.5：平台層（pod/node，分流用，非深度除錯）──────
  #   標籤是 kube_*/container_* 的 pod/container/namespace，
  #   ⚠️ 不是 service_name 也不是 application，切片要確認 container 名對得上
  platform_health:
    description: pod/node 平台層健康；只查到「app 問題 vs 平台問題」的分流深度
    datasource: prometheus
    params:
      service: {required: true, enum: service}   # 對映 container 標籤
      range:   {default: 15m, max: 1h}
    metrics:
      restarts:            # 近期重啟次數（>0 高訊號）
        expr: |
          max_over_time(kube_pod_container_status_restarts_total{container="${service}"}[${range}])
          - min_over_time(kube_pod_container_status_restarts_total{container="${service}"}[${range}])
      oomkilled:           # 是否曾 OOMKilled（>0 = 是）
        expr: max_over_time(kube_pod_container_status_last_terminated_reason{container="${service}",reason="OOMKilled"}[${range}])
      cpu_throttle_pct:    # CPU 被 throttle 比例
        unit: ratio
        expr: |
          rate(container_cpu_cfs_throttled_periods_total{container="${service}"}[${range}])
          / rate(container_cpu_cfs_periods_total{container="${service}"}[${range}])
      node_pressure:       # 所在 node 是否有 Memory/Disk pressure（>0 = 有）
        expr: max(kube_node_status_condition{condition=~"MemoryPressure|DiskPressure",status="true"})
    baseline: false        # 平台層看絕對狀態，不需基線
    output: scalar_per_metric

  # ── Step 4（日誌）/ Step 5（trace）不在 registry ──────
  #   依「資料源分流」，這兩步走 MCP（Sift / find_error_pattern_logs /
  #   find_slow_requests），由 LLM agent 直接呼叫，不經 sre_query.py。
  #   理由：log 模式聚類 / trace 瓶頸分解是 Sift 內建能力，自己用
  #   LogQL/TraceQL 重做 = 重造輪子；且封裝層只實作 scalar_per_metric
  #   一種後處理器，不背 pattern_aggregate / span_breakdown。

  # ── Step 6 / Step 0：多服務健康掃描 ──────────────────
  service_health:
    description: 一次掃多服務的 error rate / p99（RED）
    datasource: prometheus
    params:
      services: {required: true, type: list, enum: service}
      range:    {default: 15m, max: 1h}
    metrics:
      error_rate:
        unit: ratio
        expr: |
          sum by (service_name) (rate(litebank_calls_total{service_name=~"${services_regex}",span_kind="SPAN_KIND_SERVER",status_code="STATUS_CODE_ERROR"}[${range}]))
          / sum by (service_name) (rate(litebank_calls_total{service_name=~"${services_regex}",span_kind="SPAN_KIND_SERVER"}[${range}]))
      p99:
        unit: ms
        expr: histogram_quantile(0.99, sum by (service_name,le) (rate(litebank_duration_milliseconds_bucket{service_name=~"${services_regex}",span_kind="SPAN_KIND_SERVER"}[${range}])))
    output: per_service_status  # [{service, status, error_rate, p99}]
```

> 上面 `${...}` 是設計示意；實作可用 Jinja2 或簡單字串模板。重點是
> **模板固定、參數受限、聚合在程式端**，不是這個語法本身。

> ⚠️ **registry 只收數值/API 路徑的 query**（Step 0/2/3/6）。Step 4（log 模式）/ Step 5（trace 瓶頸）
> 走 MCP、由 LLM 直接呼叫，**不放進 queries.yaml**——否則封裝層就得實作 `pattern_aggregate` /
> `span_breakdown`，等於重造 Sift，違反分流原則。MCP 不可用時視為該步降級（報告註明），不在封裝層補做。

---

## 薄封裝合約（sre_query.py）

**輸入**：`--query <name>` + registry 定義的參數。
**輸出**：固定 schema 的 JSON。每次都帶 `meta`：

```json
{
  "query": "service_metrics",
  "meta": { "expr_used": "...", "range": "30m", "elapsed_ms": 240, "truncated": false },
  "result": {
    "p99":          { "current": 3840, "baseline": 450, "delta_pct": 753, "trend": "degrading", "unit": "ms" },
    "success_rate": { "current": 0.982, "baseline": 0.999, "delta_pct": -1.7, "trend": "degrading", "unit": "ratio" }
  }
}
```

**硬約束（程式碼層，prompt 擋不住的都在這）**：
1. `range > max_range` → 直接 clamp 到上限，並在 `meta.clamped=true` 標記。
2. series 數 > `max_series` → 不回 raw，報 `WINDOW_TOO_BROAD`，要求加 `http_route` 或縮窗。
3. query 逾時 `timeout_s` → 回 `TIMEOUT`，不阻塞後續步驟。
4. **`meta.expr_used` 一律回傳實際執行的查詢字串** → 報告可審計、可重跑。

**引導式錯誤（讓 LLM 一次修正，不空轉燒 token）**：
```json
{ "error": "UNKNOWN_SERVICE", "given": "transfer-service",
  "hint": "轉帳在 teller-service。合法值：[teller-service, exchange-service, user-service, ...]" }
{ "error": "NO_BASELINE", "given": "1d",
  "hint": "1d 前無資料（系統未跑滿）。可改 baseline=2h 或省略基線。" }
```

---

## 基線可行性（重要前提，已實測）

實測 Prometheus（2026-06-29）：

| offset | spanmetrics 是否有資料 |
|--------|----------------------|
| 3h     | ✅ 有 |
| 1d     | ✅ 有 |
| 7d     | ❌ 無（回 0，系統尚未跑滿 7 天） |

**結論**：report-template.md 寫的「過去 7 天同時段基線」目前不可行。設計對策：
- `baseline_offset` 預設改 **1d**，不是 7d。
- baseline 查不到資料 → 回 `NO_BASELINE` 引導式錯誤，**自動降級**為「告警窗前的同長度區間」（例如異常前 30m）當對照，報告註明基線來源。
- 之後 retention 拉長到 7d，只要改 `defaults.baseline_offset` 一個值即可。

---

## Fallback：受限的 Grafana MCP（逃生門）

除了 Step 4/5 的探索型查詢「常態」走 MCP 外，當數值工具回 `NO_DATA` 或特徵值
無法定位根因（如未知告警類型）時，也允許**額外**用 MCP 深挖。
**使用 policy（寫進 skill.md 行為規範）**：
- 僅 `query_prometheus` / `query_loki_logs` / `find_slow_requests` / `find_error_pattern_logs` / `get_assertions` 只讀工具。
- 區間不得超過 1h；發現後回主流程，不在 MCP 裡反覆試錯。
- 用了什麼查詢要寫進報告附錄（保持可審計）。

---

## 端到端案例：轉帳 burn rate Critical

告警 `BIZ-TRANSFER-BURN-CRITICAL`，`service_name=teller-service`，`http_route=/api/v1/transfers`。
示範分流如何在一次真實調查中運作。

**Step 0 — 連鎖範圍預掃 → API**
```
service_health --services teller-service,transaction-service,account-service --range 15m
→ teller p99=3800 err=4% / transaction p99=3600 err=3.8%(也在燒) / account p99=48 正常
結論：非單點，下游 transaction-service 跟著爛；account 正常，排除查詢層。
```

**Step 2 — 業務指標 + 基線 → API**
```
service_metrics --service teller-service --http-route /api/v1/transfers --range 30m --baseline 1d
→ p99 current=3800 baseline=420 (+805%) / rps current=47 baseline=45 (+4%)
結論：p99 暴增 9 倍但 rps 幾乎沒動 → 不是流量暴衝，是內部劣化。
（此處正是 baseline 的價值：沒有 rps 基線就分不出 symptom vs cause）
```

**Step 3 — 基礎設施 → API**
```
infra_metrics --service transaction-service --range 15m
→ hikaricp_pending=14 (閾值>10=Critical) / hikaricp_util=1.0 / heap=62% / tomcat=55%
結論：transaction-service 連線池爆滿且排隊 14 → DB 連線耗盡嫌疑，信心拉高。
```

**Step 4 — 錯誤日誌模式 → MCP（Sift）** ⭐
```
find_error_pattern_logs  service=transaction-service  range=15m
→ "Connection is not available, request timed out after 30000ms" ×312
  "Unable to acquire JDBC Connection"                            ×288
結論：錯誤訊息坐實 Step 3 假設——HikariCP 連線拿不到。
（找重複模式是 Sift 拿手，自己寫聚類費工，故走 MCP）
```

**Step 5 — 慢 trace 瓶頸 → MCP（Sift）** ⭐
```
find_slow_requests  service=teller-service  range=15m
→ teller /api/v1/transfers 3780ms
    └ transaction POST /debit 3700ms ← 瓶頸
         └ 等待 DB 連線 3300ms ← 卡在這
              └ db.query UPDATE accounts 180ms
結論：時間全花在「等連線」，非 SQL 本身慢。三層證據閉環。
```

**報告根因（信心 90%）**：transaction-service HikariCP 連線池耗盡（pending 14），
轉帳 debit 卡在等連線（trace 3.3s 在 acquire connection），log 確認 "Connection timed out" ×312。
**非流量問題**（rps 持平）。立即行動：查 transaction-service 是否有慢查詢/連線洩漏佔住池子，臨時調大 pool 止血。

> 此案例同時印證：**基線**幫 LLM 在 Step 2 排除「流量暴衝」；**分流**讓數值步驟（0/2/3）拿到可控可審計的數字、模式步驟（4/5）省下重造 Sift 的工。

---

## skill.md 調查流程對映（改寫後）

| Step | 工具呼叫 | 路徑 | 性質 |
|------|---------|------|------|
| 0 預掃 | `service_health --services <告警服務+上下游>` | **API** | 數值 |
| 2 業務指標 | `service_metrics --service --http-route --range --baseline` | **API** | 數值+基線 |
| 3 app 資源 | `infra_metrics --service` | **API** | 數值 |
| 3.5 平台層 | `platform_health --service`（pod/node 分流） | **API** | 數值 |
| 4 日誌 | `find_error_pattern_logs`（MCP / Sift） | **MCP** | 找模式 |
| 5 Trace | `find_slow_requests`（MCP / Sift，Critical 必跑） | **MCP** | 找瓶頸 |
| 6 依賴健康 | `service_health`（Step 0 已含，視情況複查） | **API** | 數值 |

> **Step 3 與 3.5 要一起讀**：heap 高「且」node MemoryPressure/OOMKilled → 根因偏平台；
> heap 高但 node 正常 → 根因偏 app。這個對照是 `platform_health` 的價值所在。

---

## 待確認決策（實作前需拍板）

1. **封裝語言**：Python（建議，pattern 聚合/baseline 計算好寫）vs Bash+jq（更輕但聚合難）。
2. **Grafana token**：需要一個 read-only service account token；放環境變數還是 skill 設定？
3. **Loki datasource**：`loki` vs `loki-1` 哪個有 lite-bank 日誌、`service_name` 標籤怎麼帶 → 實作前先打一條確認。
4. **registry 形態起步**：封裝層做 4 條數值 query（`service_metrics` / `infra_metrics` / `platform_health` / `service_health`），共用 `scalar_per_metric` 一種後處理器；log/trace 走 MCP。這樣即可覆蓋現有 35 條告警的調查需求；先不做 Z-Score 異常檢測（避免過度工程化，後期再加）。

---

## 實作切片要釘死的契約（開工清單）

以下幾項目前仍是「示意」，**不在紙上定，由 `service_metrics` 垂直切片實測釘死**，
之後其餘工具沿用同契約。切片完成 = 這些全部有定案，才動 skill.md。

### 前置阻塞（紙上定不了，先解）
- [ ] **Grafana read-only token**：service account token 怎麼產、放哪（環境變數 `GRAFANA_TOKEN`？）
- [ ] **Loki datasource**：`loki`（uid `bffr2l3ogrtvkd`）vs `loki-1`（uid `efq2t31rpkv7ka`）哪個有 lite-bank 日誌、`service_name` 標籤帶不帶得到
- [ ] **kube_* 標籤對映**：`platform_health` 用的 `container=="<service>"` 是否對得上（kube_*/container_* 標籤是 pod/container/namespace，與 service_name/application 都不同）
- [ ] **datasource proxy 連通**：`/api/datasources/proxy/uid/prometheus/api/v1/query` 打得通

### 切片要產出的契約
- [ ] **CLI 介面**：完整 arg 命名（`--query/--service/--http-route/--range/--baseline`）與預設
- [ ] **輸出 JSON schema**：定案 `{query, meta{expr_used,range,clamped,elapsed_ms,truncated}, result{...}}`
- [ ] **模板引擎**：Jinja2 vs 簡單字串（建議 Jinja2，條件式 route filter 好寫）
- [ ] **baseline 降級演算法**：1d 無資料 → 回 `NO_BASELINE` → 自動改用「告警窗前同長度區間」，`meta.baseline_source` 標明來源
- [ ] **完整錯誤碼表**：`UNKNOWN_SERVICE` / `WINDOW_TOO_BROAD` / `TIMEOUT` / `NO_DATA` / `NO_BASELINE`（各帶 `hint`）
- [ ] **clamp 行為**：`range>max_range` 時 clamp + `meta.clamped=true`

### 切片驗收標準
- [ ] 拿真實告警（如轉帳 burn rate）跑 `service_metrics`，回傳含 current/baseline/delta/trend
- [ ] 故意傳錯服務名 → 回引導式錯誤含合法值清單
- [ ] 故意傳 `--range 10d` → 自動 clamp 到 3h 且標記
- [ ] 8s timeout 生效，不阻塞

---

## 維護成本權衡

- **固化的是「已穩定的 domain」**：35 條告警 + 6 個 SLO 已定案，查詢邏輯不會常變，固化划算。
- **改一條查詢 = 改 registry 一個欄位**，不動程式邏輯。
- **新增告警類型** → registry 加一筆 entry；真的沒覆蓋到的，fallback MCP 兜底。
- 避免的成本：純 prompt 方案那種「反覆調 prompt 堵 LLM 幻覺」的無底洞。
