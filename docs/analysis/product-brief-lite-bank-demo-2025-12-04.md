---
stepsCompleted: [1, 2, 3, 4, 5]
inputDocuments: []
workflowType: 'product-brief'
lastStep: 5
project_name: 'lite-bank-demo'
user_name: '主人'
date: '2025-12-04'
---

# Product Brief: lite-bank-demo

**Date:** 2025-12-04
**Author:** 主人

---

## Executive Summary

**lite-bank-demo** 是一個展示現代可觀測性最佳實踐的微服務範例專案,旨在證明完整的 log ↔ trace ↔ metric 三本柱整合如何徹底改變微服務系統的問題排查效率。透過模擬真實銀行業務場景(多幣別帳戶、存提款、轉帳、換匯),結合 SAGA 分散式事務模式、Chaos Mesh 混沌工程,以及從前端到後端的完整 trace propagation,本專案為團隊展示從傳統 10-15 分鐘人工排查到 1-2 分鐘自動化定位的效率躍升,並為未來 AI SRE(LLM 根因分析)奠定基礎。

**核心價值主張:** 透過完整的手動 SDK instrumentation(包含 Quarkus)、SAGA 模式、跨語言 trace propagation(Java SpringBoot + Quarkus + Python)、異步通訊追蹤(Kafka KRaft)、以及前端 OpenTelemetry,展示可觀測性在複雜微服務系統中的決定性價值,推動團隊從 auto-instrumentation 失敗經驗轉向完整實作,建立生產等級的可觀測性標準。

---

## Core Vision

### Problem Statement

**當前痛點:** SRE 和開發人員在排查微服務系統問題時,平均需要 10-15 分鐘才能定位根因。團隊依賴 GrayLog 和 Grafana,透過自定義 request ID 手動追蹤 LOG,再透過時間排序推斷微服務流轉順序,最後人工關聯 Metrics。這個過程不僅耗時,且容易出錯。

**根本問題:**
- **LOG 與 Metrics 完全分離**: 缺乏自動關聯,需人工判斷時間點和因果關係
- **LOG 格式不統一**: 每個微服務使用不同格式,難以建立統一 Parser
- **缺乏完整鏈路視圖**: 需手動拼湊服務調用順序,無法直觀看到完整 trace
- **Auto-instrumentation 的侷限**: 之前嘗試導入 OpenTelemetry,但因 Quarkus 不支援 auto-instrumentation,導致鏈路不完整,投資報酬率低,團隊信心受挫

### Problem Impact

**對 SRE 團隊的影響:**
- 每次故障排查耗費 10-15 分鐘,在高負載或緊急事件時造成巨大壓力
- 人工拼湊服務關聯容易遺漏關鍵環節,導致誤判或延遲修復
- 缺乏視覺化服務拓撲,難以快速理解複雜系統的依賴關係
- 無法有效利用可觀測性工具的潛力

**對開發團隊的影響:**
- 難以重現和定位生產環境問題
- 缺乏精確的效能瓶頸資訊,優化方向不明確
- 對 SDK instrumentation 的抗拒源於缺乏成功案例和可見價值
- 分散式事務(如 SAGA)的除錯極度困難

**對組織的影響:**
- MTTR(平均修復時間)過高影響服務可用性
- 團隊對可觀測性工具投資缺乏信心(auto-instrumentation 失敗經驗)
- 無法充分利用 AI/LLM 技術進行智能維運(缺乏結構化可觀測性資料)
- 複雜微服務系統的維護成本持續上升

### Why Existing Solutions Fall Short

**傳統 LOG 中心化方案(當前使用):**
- ✗ 依賴自定義 request ID,需額外開發和維護成本
- ✗ 時間排序無法準確反映因果關係(網路延遲、時鐘偏移問題)
- ✗ LOG、Metrics、Traces 分離,缺乏統一 context
- ✗ 難以處理跨語言、跨框架的分散式追蹤
- ✗ 異步通訊(Message Queue)的追蹤幾乎不可能
- ✗ 分散式事務(SAGA)的流程追蹤非常困難

**OpenTelemetry Auto-instrumentation(曾嘗試):**
- ✗ 不支援所有框架(如 Quarkus),導致鏈路缺口,整體價值大幅降低
- ✗ 自動採集資料粒度不足,缺乏業務語義(如帳戶 ID、交易金額)
- ✗ 採樣策略難以配置(頭採樣 vs 尾採樣的取捨)
- ✗ 團隊因不完整效果而失去信心,認為可觀測性投資不值得

**市面上 OpenTelemetry Demo:**
- ✗ 多為電商、訂單等通用場景,缺乏金融領域複雜性
- ✗ 缺少分散式事務(SAGA)的追蹤展示
- ✗ 未整合混沌工程,無法展示真實故障場景下的排查能力
- ✗ 未探索 AI SRE,停留在傳統可觀測性層面
- ✗ 未展示多語言 SDK 完整實作(尤其是 Quarkus)
- ✗ 缺少前端到後端的完整追蹤
- ✗ 缺少異步通訊(Message Queue)的 trace propagation

### Proposed Solution

**lite-bank-demo** 透過完整的技術棧和真實場景,展示可觀測性三本柱的完整威力:

**核心解決方案:**

**1. 完整的手動 SDK Instrumentation**
   - **微服務語言組合**:
     - Java SpringBoot(主流技術棧):API Gateway、Auth、User、Transaction、Deposit/Withdrawal
     - Java Quarkus(展示 auto-instrumentation 盲點):Account、Currency Exchange
     - Python(跨語言 propagation):Transfer(SAGA Orchestrator)、Exchange Rate、Notification
   - **完整實作 OpenTelemetry SDK**,確保鏈路無缺口
   - **自定義 span attributes** 包含業務語義:
     - 帳戶資訊:`account.id`, `account.currency`, `account.balance`
     - 交易資訊:`transaction.id`, `transaction.amount`, `transaction.type`
     - 匯率資訊:`exchange.from_currency`, `exchange.to_currency`, `exchange.rate`

**2. 真實銀行業務場景 + SAGA 分散式事務**
   - **核心業務功能**:
     - 多幣別帳戶管理(台幣、美金、日幣)
     - 存款/提款操作
     - 轉帳(含跨幣別轉帳)
     - 換匯(即時匯率查詢)

   - **SAGA 模式完整實作**(Transfer Service 編排):
     ```
     轉帳 SAGA 流程:
     1. 凍結來源帳戶金額 (Account Service)
     2. 查詢匯率(如跨幣別) (Exchange Rate Service)
     3. 扣除來源帳戶 (Account Service)
     4. 增加目標帳戶 (Account Service)
     5. 記錄交易 (Transaction Service)
     6. 發送通知事件 (Kafka → Notification Service)

     失敗補償(Compensating Transactions):
     - 步驟 3 失敗 → 解凍來源帳戶
     - 步驟 4 失敗 → 退回來源帳戶
     - 步驟 5 失敗 → 回滾所有帳戶操作
     ```

   - **可觀測性價值**:
     - Trace 清楚顯示 SAGA 的每一步執行順序
     - 失敗時能看到在哪一步失敗,以及 compensation 是否執行
     - 這是**完整 trace 的殺手級展示場景**!

**3. 微服務架構(10 個服務)**

   **banking-services namespace:**
   - **API Gateway** (SpringBoot) - 統一入口,trace context 注入起點
   - **Auth Service** (SpringBoot) - JWT 認證與授權
   - **User Service** (SpringBoot) - 使用者資料管理
   - **Account Service** (Quarkus) ⭐ - 多幣別帳戶、餘額管理
   - **Transaction Service** (SpringBoot) - 交易記錄與歷史查詢
   - **Transfer Service** (Python) ⭐ - SAGA 編排器,轉帳流程編排
   - **Deposit/Withdrawal Service** (SpringBoot) - 存提款操作
   - **Exchange Rate Service** (Python) - 即時匯率查詢(模擬外部 API)
   - **Currency Exchange Service** (Quarkus) ⭐ - 換匯業務編排
   - **Notification Service** (Python) - 異步通知處理(Kafka Consumer)

**4. Chaos Mesh 混沌工程整合**
   - **預定義故障場景**(YAML 配置):
     - **NetworkChaos**: Exchange Rate Service 網路延遲 200ms → Transfer 超時
     - **NetworkPartition**: Account Service 與 Transaction Service 斷線
     - **PodChaos (kill)**: Transfer 處理中 Pod 突然死亡
     - **PodChaos (failure)**: Transaction Service 容器失效
     - **StressChaos (CPU)**: Account Service CPU 100% → 回應變慢
     - **StressChaos (Memory)**: Transaction Service OOM
   - **即時觸發**: `kubectl apply -f chaos-scenarios/`
   - **展示價值**: 真實故障場景下,完整可觀測性如何快速定位根因

**5. log ↔ trace ↔ metric 三向跳轉**
   - **Grafana Stack 完整整合**:
     - **Tempo** - 分散式追蹤儲存與查詢
     - **Loki** - 日誌聚合
     - **Prometheus** - 指標採集
     - **OpenTelemetry Collector** - 統一資料收集
   - **統一 trace context** 自動關聯所有可觀測性資料
   - **一鍵跳轉**:
     - 從 trace ID → 相關 logs(同一 trace_id)
     - 從 trace span → 該服務的 metrics(時間點對齊)
     - 從 logs → trace 完整鏈路
     - 從 metrics 異常 → trace 定位問題請求

**6. 前端到後端完整可觀測性**
   - **前端技術棧**:
     - React + TypeScript + Material-UI
     - **OpenTelemetry Browser SDK** ⭐
   - **UI 功能頁面**:
     - 登入頁(認證流程)
     - 儀表板(多幣別帳戶總覽、圖表視覺化)
     - 轉帳頁(來源/目標帳戶、金額輸入)
     - 換匯頁(幣別選擇、即時匯率顯示)
     - 存提款頁(帳戶選擇、操作類型)
     - 交易歷史頁(交易記錄列表)
   - **完整 trace 鏈路**:
     ```
     使用者點擊「轉帳」按鈕
       ↓ (Browser 生成 trace_id)
     React App (OpenTelemetry Browser SDK)
       ↓ (HTTP request 帶 traceparent header)
     API Gateway
       ↓
     Auth Service → Transfer Service → Account/Exchange Rate/Transaction
       ↓
     Kafka → Notification Service
     ```
   - **展示價值**:
     - 從使用者操作到微服務的**完整可觀測性**
     - 錯誤發生時,UI 顯示 trace_id,SRE 可直接查詢
     - 這是**終極可觀測性展示**!

**7. 異步通訊的 trace propagation**
   - **Kafka KRaft mode** (無需 ZooKeeper,2025 年最佳實踐)
   - **訊息佇列 trace 傳播**:
     ```
     Transfer Service 完成轉帳
       ↓ (發送 Kafka 訊息,帶 trace context)
     Kafka Topic: banking.notifications
       ↓ (Notification Service 消費訊息)
     Notification Service 處理通知
       ↓ (繼承同一 trace_id)
     完整 trace 包含異步流程
     ```
   - **展示價值**:
     - 異步通訊不再是追蹤的黑洞
     - 跨 Message Queue 的完整鏈路視圖

**8. AI SRE 前瞻探索(Phase 2)**
   - **架構設計**:
     ```
     Grafana Alerting 觸發
       ↓ (Webhook 帶 trace_id)
     AI SRE Agent
       ├─ 從 Tempo 取得 Trace (span 關係 = 服務拓撲)
       ├─ 從 Loki 取得錯誤 Logs (trace_id 關聯)
       └─ 從 Prometheus 取得關鍵 Metrics (GC, JVM, Latency)
       ↓ (結構化資料整理)
     LLM Prompt (服務關聯圖 + 錯誤資訊 + 指標異常)
       ↓
     根因分析報告 + 建議解決方案
     ```
   - **LLM 選擇**: GPT-4o / Claude 3.5 Sonnet / 本地模型 GPT-oss 120b
   - **展示場景**:
     - 單一服務錯誤 → LLM 指出錯誤服務與原因
     - 連鎖失敗 → LLM 追溯根因(如 Exchange Rate 慢 → Transfer 超時)
   - **定位**: 輔助 SRE,提供問題頭緒和方向,非完全自動化

**技術架構總覽:**

**Kubernetes 部署(單叢集,多 Namespace):**
```yaml
banking-services namespace:
  ├── api-gateway (SpringBoot)
  ├── auth-service (SpringBoot)
  ├── user-service (SpringBoot)
  ├── account-service (Quarkus)
  ├── transaction-service (SpringBoot)
  ├── transfer-service (Python)
  ├── deposit-withdrawal-service (SpringBoot)
  ├── exchange-rate-service (Python)
  ├── currency-exchange-service (Quarkus)
  └── notification-service (Python)

observability namespace:
  ├── grafana
  ├── tempo
  ├── loki
  ├── prometheus
  └── otel-collector

messaging namespace:
  └── kafka (KRaft mode)

chaos namespace:
  └── chaos-mesh
```

**理想排查流程(Demo 展示):**
```
傳統方式(10-15 分鐘):
1. 查看 GrayLog 錯誤訊息
2. 找到 request ID
3. 在多個微服務的 LOG 中搜尋
4. 透過時間戳排序推測順序
5. 猜測相關的 Metrics
6. 人工判斷根因

完整可觀測性(1-2 分鐘):
1. Grafana 收到告警 → 包含 trace_id
2. 點擊 trace_id → 看到完整服務調用鏈路和錯誤節點
3. 從錯誤 span 跳轉到該服務的 logs
4. 查看該時間點的 metrics 確認資源狀態
5. 根因明確!
6. (Phase 2) LLM 自動分析,給出報告和建議
```

### Key Differentiators

**相比傳統方案:**
- ✅ **10 倍效率提升**: 從 10-15 分鐘降至 1-2 分鐘
- ✅ **視覺化衝擊**: 完整服務拓撲圖和鏈路追蹤,告別人工拼湊
- ✅ **統一 Context**: 所有可觀測性資料自動關聯,無需人工判斷時間點
- ✅ **業務語義**: Trace 包含業務資訊(帳戶、金額、幣別),不只是技術指標
- ✅ **分散式事務可追蹤**: SAGA 流程和補償邏輯完整可視化

**相比 Auto-instrumentation:**
- ✅ **零鏈路缺口**: 手動 SDK 確保所有服務(包含 Quarkus)完整追蹤
- ✅ **自定義資料粒度**: 可加入業務關鍵指標,提升排查精準度
- ✅ **可控採樣策略**: 靈活配置尾採樣,兼顧成本和完整性
- ✅ **團隊信心重建**: 透過成功案例證明投資價值

**相比市面 OpenTelemetry Demo:**
- ✅ **SAGA 模式展示**: 分散式事務的可觀測性殺手級場景
- ✅ **前端 OpenTelemetry**: 從使用者操作到微服務的完整追蹤
- ✅ **Kafka 異步追蹤**: 展示訊息佇列的 trace propagation
- ✅ **Chaos Mesh 整合**: 真實故障場景(非刻意設計的 Demo 錯誤)
- ✅ **AI SRE 前瞻性**: 探索 LLM 根因分析,引領智能維運趨勢
- ✅ **金融場景複雜度**: 多幣別、換匯、SAGA,貼近真實業務
- ✅ **多語言完整實作**: SpringBoot + Quarkus + Python,展示跨語言能力
- ✅ **生產等級架構**: Kafka KRaft、K8s 多 Namespace、美觀 UI

**獨特價值:**
- 🎯 **證明 SDK 投資價值**: 透過視覺化對比,說服團隊接受手動 instrumentation
- 🎯 **SAGA 模式教學**: 學習分散式事務的實作與除錯
- 🎯 **金融知識普及**: 團隊順便學習銀行業務邏輯和領域概念
- 🎯 **AI SRE 基礎**: 為未來智能維運奠定可觀測性資料基礎
- 🎯 **內部標準參考**: 成為公司可觀測性最佳實踐的範本
- 🎯 **技術深度**: 涵蓋前端、後端、異步、混沌工程、AI 的完整技術棧

**為什麼是現在?**
- ✅ **AI/LLM 技術成熟**,讓 AI SRE 從概念變為可能
- ✅ **團隊經歷 auto-instrumentation 失敗**,需要看到完整方案的價值
- ✅ **可觀測性三本柱工具鏈成熟**(Grafana Stack + OpenTelemetry)
- ✅ **Kafka KRaft 生產就緒**,架構更簡化
- ✅ **組織開始重視 MTTR 和系統穩定性**,願意投資可觀測性
- ✅ **微服務複雜度上升**,傳統 LOG 方案已無法應對

**成功指標:**
- 🎯 SRE 團隊認同可觀測性三本柱的價值
- 🎯 開發團隊願意在專案中導入手動 SDK
- 🎯 理解 SAGA 模式在分散式追蹤中的價值
- 🎯 (加分)對 AI SRE 產生興趣並開始探索
- 🎯 成為公司內部可觀測性的標準參考架構

---

## Target Users

### Primary Users: 初階 SRE 與開發人員(種子培養對象)

**使用者背景:**
- **經驗等級**: 約 1 年工作經驗的 SRE 或開發人員
- **技術能力**:
  - 僅有基本微服務概念,未深入實作過分散式系統
  - 已使用 Grafana,能建立簡單 Dashboard,略懂 PromQL 查詢
  - 知道 LOG、Metrics 的基本用途,但不理解 Trace 的價值
  - 對 OpenTelemetry、SAGA、Chaos Engineering 僅有名詞了解
- **當前痛點**:
  - 每月遇到幾次微服務問題需排查,依賴資深同事協助
  - 不知道如何在大型系統中快速定位問題
  - 對可觀測性工具的潛力認知不足

**使用情境:**
- **接觸場景**: 公司內部小型技術分享會(workshop 形式)
- **參與人數**: 小組形式,非大規模培訓
- **後續支援**: **無後續支援機制**,一次性展示
- **決策權限**: **本人無導入決策權**,決策在管理層
- **組織環境**:
  - 存在政治性阻礙,短期內難以推動新技術導入
  - 管理層對投資新工具持保守態度(過去有失敗經驗)
  - 團隊對 auto-instrumentation 失敗經驗心有餘悸

**核心需求:**
- **視覺化衝擊 > 技術細節**: 不需要深入教學,需要「看到」完整可觀測性的威力
- **風險管理意識**: 理解重大事件若發生可能導致裁罰(監管單位或客戶)
- **未來準備**: 當政治環境改變或重大事件發生時,能夠想起「這個方案」
- **信心建立**: 透過成功案例重建對 SDK instrumentation 的信心
- **生涯發展**: 累積先進技術知識,為未來技術轉型做準備

**成功願景 - "Aha! Moment":**

當初階 SRE/開發人員在 workshop 中看到:
1. **視覺化對比**:
   - 傳統方式:跨 3 個視窗、手動搜尋 LOG、猜測時間點,10-15 分鐘
   - 完整可觀測性:點擊 trace_id → 完整鏈路圖 → 錯誤定位,1-2 分鐘

2. **SAGA 可追蹤性**:
   - Grafana Tempo 顯示轉帳流程的 6 個步驟
   - 失敗時看到在步驟 4 失敗,補償邏輯自動執行
   - 心中想:**「原來分散式事務可以這樣 debug!」**

3. **Chaos 真實故障**:
   - 即時觸發 NetworkChaos → Exchange Rate 延遲 200ms
   - Trace 自動顯示 timeout 發生在哪個 span
   - 心中想:**「這不是 demo 錯誤,是真實故障!」**

4. **前端到後端完整鏈路**:
   - 從 React UI 點擊按鈕 → API Gateway → 10 個微服務 → Kafka → 通知
   - 一條 trace 串起整個流程
   - 心中想:**「這才是終極可觀測性!」**

5. **AI SRE 一瞥未來**(Phase 2):
   - LLM 自動分析 trace + logs + metrics,產生根因報告
   - 心中想:**「這是未來的方向!」**

**使用者旅程:**

```
1. 發現階段 (Discovery)
   └─ 收到內部 workshop 通知:「可觀測性三本柱展示」
   └─ 半信半疑參加(過去 auto-instrumentation 失敗經驗)

2. Demo 體驗 (Experience)
   └─ 看到傳統方式 vs 完整可觀測性的對比
   └─ 目睹 Chaos Mesh 觸發真實故障 → trace 即時定位
   └─ 看到 SAGA 流程的完整視覺化
   └─ 看到前端到後端的完整鏈路追蹤
   └─ (Phase 2) 看到 AI SRE 自動分析

3. 頓悟階段 (Enlightenment - "Aha! Moment")
   └─ **「原來這就是可觀測性的真正價值!」**
   └─ **「如果我們有這個,上次的重大事件不會拖這麼久!」**
   └─ **「這不只是工具,是解決問題的方式!」**
   └─ 心中種子被種下:「未來如果有機會,我想推動這個!」

4. 評估階段 (Evaluation)
   └─ 離開 workshop 後,與同事討論
   └─ 理解短期無法導入(政治、預算、優先級)
   └─ 但技術願景已經理解並記住

5. 潛在導入階段 (Potential Adoption - 中長期)
   └─ 情境 A:重大事件發生 → 管理層重視 → 回想起這個方案
   └─ 情境 B:政治環境改變 → 新技術導入窗口開啟 → 提出這個方案
   └─ 情境 C:個人轉職到更開放的組織 → 帶著這個知識推動
   └─ 情境 D:主動在小範圍專案試驗 → 積累經驗等待時機
```

### Secondary Users: 資深 SRE 與技術決策者(未來潛在推動者)

**角色定位:**
- **資深 SRE/Architect**: 有能力評估技術方案的完整性與可行性
- **技術 Lead/Manager**: 有一定決策影響力,但仍受組織政治限制
- **對可觀測性的認知**: 理解理論,但缺乏完整實作經驗和成功案例

**對 Demo 的期待:**
- 評估技術方案的**生產可行性**(不只是 POC 或 demo)
- 理解投資成本 vs 效益(MTTR 降低、人力節省、風險降低)
- 尋找內部技術標準的參考範本
- 當時機成熟時,有完整方案可參考和推動

**價值獲取:**
- **技術深度**: 理解 SDK 手動 instrumentation、SAGA 追蹤、Kafka propagation 的實作細節
- **ROI 評估**: 看到 10-15 分鐘 → 1-2 分鐘的效率提升
- **風險管理**: 理解完整可觀測性如何降低重大事件的裁罰風險
- **技術趨勢**: 看到 AI SRE 的未來方向,提前做準備

**成功指標:**
- 認同「這是正確的技術方向」
- 理解「為什麼 auto-instrumentation 不夠」
- 當組織環境改變時,能夠成為推動者
- 將這個方案納入未來技術規劃的候選清單

### 使用者與產品定位的關鍵洞察

**這不是「說服工具」,而是「埋下種子」:**

傳統產品思維會假設:
- ❌ Demo 後立即導入
- ❌ 使用者有決策權和推動力
- ❌ 組織準備好接受新技術

**真實情況:**
- ✅ **短期目標**:讓初階 SRE/開發人員看到「未來的樣子」
- ✅ **中期目標**:當重大事件發生或環境改變時,他們會想起這個方案
- ✅ **長期目標**:這些人成長為技術決策者時,成為推動者
- ✅ **風險管理**:讓團隊理解完整可觀測性如何避免裁罰風險

**產品策略調整:**
- 強調**視覺化衝擊**而非技術細節教學(workshop 無後續支援)
- Demo 要展示**真實故障場景**(Chaos Mesh),不只是刻意設計的錯誤
- 要讓人看到**10 倍效率差異**(傳統 10-15 分鐘 vs 可觀測性 1-2 分鐘)
- AI SRE 展示**未來願景**,激發「這是趨勢」的認知
- 完整架構展示**生產等級可行性**,不只是玩具 demo

**成功定義的三個時間尺度:**

**短期成功(Workshop 結束時):**
- ✅ 參與者驚嘆「原來可以這樣!」
- ✅ 理解完整可觀測性的價值和威力
- ✅ 對 SDK instrumentation 重拾信心
- ✅ 記住「未來如果有機會,這是正確方向」

**中期成功(3-12 個月):**
- ✅ 重大事件發生時,團隊想起「如果有那個方案就好了」
- ✅ 管理層因事件壓力,開始重視可觀測性投資
- ✅ 初階使用者成長,開始在小範圍試驗
- ✅ 內部技術討論時,有人提起「我們看過的那個 demo」

**長期成功(1-3 年):**
- ✅ 組織環境改變,技術導入窗口開啟
- ✅ 當年的初階使用者成為推動者
- ✅ Demo 成為組織內部可觀測性標準參考
- ✅ 完整技術棧被部分或全部採用

---

## Success Metrics

### 使用者成功指標:種子發芽的跡象

**定位:** 這不是傳統產品的轉化率或採用率,而是「技術啟蒙」的影響力指標。我們不追蹤數據,但如果以下情況發生,代表種子成功發芽。

**短期可觀察指標(Workshop 結束後):**

- ✅ **私下技術詢問** - 有參與者主動詢問技術實作細節
  - 詢問 OpenTelemetry SDK 如何手動 instrumentation
  - 詢問 SAGA 補償邏輯如何實作
  - 詢問 Chaos Mesh 如何整合到現有環境
  - 詢問 Kafka trace propagation 的技術細節

- ✅ **技術視野擴展** - 參與者理解完整可觀測性的可能性和價值
  - 從「只知道 Grafana 看 metrics」到「理解 log ↔ trace ↔ metric 三本柱價值」
  - 從「auto-instrumentation 失敗經驗」到「理解為什麼需要手動 SDK」
  - 從「Trace 是什麼?」到「原來 Trace 這麼強大!」

- ✅ **概念記憶留存** - 離開時記住「有這種完整方案存在」
  - 記住「10-15 分鐘 → 1-2 分鐘」的效率差異
  - 記住「真實故障場景下的可觀測性威力」
  - 記住「未來如果有機會,這是正確的方向」

**中長期影響(不追蹤,但如果發生代表成功):**

- 🌱 **內部擴散** - 有人在內部技術論壇或 Slack 提起這個 demo
- 🌱 **方案想起** - 有人在新專案規劃時想起這個方案
- 🌱 **跨組織傳播** - 有人轉職到新公司後,嘗試推動類似架構
- 🌱 **同行認可** - 懂的人欣賞技術細節的完整性(SAGA、真實故障場景、生產等級架構)

**非目標(明確排除):**

這些**不是**成功的衡量標準:
- ❌ Demo 後的實際導入數量
- ❌ 組織層面的技術採用率
- ❌ 可量化的 ROI 或轉化率
- ❌ 追蹤種子發芽的數據系統
- ❌ 後續支援或培訓成效

**原因:** 這是技術啟蒙,不是商業產品。種子會跟著人走,影響力自然擴散。就像看完一場精彩的 TED Talk,我們不需要追蹤觀眾後續行為,但思想的種子已經埋下。

---

### 個人目標:技術傳道者視角

**Demo 執行成功標準:**

✅ **功能完整展示** - 所有核心功能順利演示:

1. **log ↔ trace ↔ metric 三向跳轉**
   - 從 Grafana Tempo trace 跳轉到 Loki logs
   - 從 logs 跳轉回完整 trace 鏈路
   - 從 metrics 異常跳轉到對應 trace
   - 展示統一 trace context 的威力

2. **SAGA 流程完整追蹤**
   - Grafana Tempo 顯示轉帳 SAGA 的 6 個步驟
   - 正常流程:凍結 → 查匯率 → 扣款 → 入帳 → 記錄 → 通知
   - 失敗流程:展示補償邏輯(Compensating Transactions)執行
   - Trace span 清楚標記每個步驟的執行狀態和耗時

3. **Chaos Mesh 真實故障場景**
   - 即時觸發 NetworkChaos:Exchange Rate Service 延遲 200ms
   - Trace 自動顯示 timeout 發生在哪個 span
   - 展示「這不是刻意設計的 demo 錯誤,而是真實故障注入」
   - 其他場景:Pod kill、CPU stress、網路分區等

4. **前端到後端完整鏈路**
   - 從 React UI 點擊「轉帳」按鈕(OpenTelemetry Browser SDK)
   - 經過 API Gateway → Auth → Transfer → Account/Exchange Rate/Transaction
   - 通過 Kafka → Notification Service
   - 一條完整 trace 串起整個流程(前端 → 後端 → 異步)

5. **Kafka 異步通訊追蹤**
   - 展示訊息佇列的 trace context propagation
   - Notification Service 繼承 Transfer Service 的 trace_id
   - 證明「異步通訊不再是追蹤的黑洞」

6. **(Phase 2) AI SRE 根因分析**
   - LLM 自動從 Tempo + Loki + Prometheus 取得資料
   - 產生根因分析報告和建議解決方案
   - 展示「AI 時代的可觀測性未來」

---

**技術學習與複習:**

✅ **OpenTelemetry 深度掌握**
- 手動 SDK instrumentation 實作(SpringBoot + Quarkus + Python)
- Trace context propagation 跨服務傳遞
- 自定義 span attributes 加入業務語義
- 採樣策略設計(尾採樣 vs 頭採樣)

✅ **SAGA 模式實作**
- Orchestration-based SAGA(Transfer Service 作為協調者)
- Compensating transactions 補償邏輯設計
- 分散式事務的可觀測性挑戰與解決方案

✅ **Chaos Engineering**
- Chaos Mesh 故障注入場景設計
- NetworkChaos、PodChaos、StressChaos 實作
- 可觀測性與混沌工程的結合

✅ **Kafka trace propagation**
- 跨訊息佇列的 trace context 傳遞
- Kafka KRaft mode 架構理解
- Producer 和 Consumer 的 instrumentation

✅ **Grafana Stack 整合**
- Tempo(traces)、Loki(logs)、Prometheus(metrics)整合
- OpenTelemetry Collector 資料收集與路由
- 統一 trace context 實現三本柱關聯

---

**技術完整性標準:現實情境模擬**

✅ **金融交易真實性**

**同步處理(符合金融業務需求):**
- 轉帳/換匯採用**同步處理**(非異步)
- 原因:金融交易需要即時確認結果,不能接受「稍後處理」
- 使用者在 UI 點擊「轉帳」後,必須立即知道成功或失敗
- 這與電商「下單後異步處理」的場景完全不同

**完整 SAGA 模式:**
```
1. 凍結來源帳戶金額 (Account Service) - 確保資金可用
2. 查詢匯率(如跨幣別) (Exchange Rate Service) - 同步查詢,可能超時
3. 扣除來源帳戶 (Account Service) - 原子操作
4. 增加目標帳戶 (Account Service) - 原子操作
5. 記錄交易 (Transaction Service) - 持久化交易記錄
6. 發送通知事件 (Kafka → Notification Service) - 唯一異步環節

補償流程(Compensating Transactions):
- 步驟 3 失敗 → 解凍來源帳戶
- 步驟 4 失敗 → 退回來源帳戶
- 步驟 5 失敗 → 回滾所有帳戶操作
- 步驟 6 失敗 → 不影響交易完成,僅記錄通知失敗
```

**Kafka 異步僅用於通知:**
- **正確使用場景**:Notification Service(簡訊、Email、推播通知)
- **為什麼**:通知失敗不影響交易完成,可以稍後重試
- **錯誤使用場景**:轉帳、換匯不能用異步(必須即時確認)
- **真實性**:符合金融系統的實際設計模式

---

✅ **生產等級架構**

**Kafka KRaft mode(2025 最佳實踐):**
- 無需 ZooKeeper,簡化架構
- Kafka 4.0+ 原生 KRaft 模式
- 符合最新技術趨勢

**K8s 多 Namespace 隔離:**
```yaml
banking-services:    # 業務服務
observability:       # 可觀測性工具
messaging:           # Kafka
chaos:              # Chaos Mesh
```

**完整錯誤處理與補償邏輯:**
- 每個 SAGA 步驟都有對應的補償邏輯
- Timeout 處理、重試機制、冪等性設計
- 錯誤狀態在 trace 中清楚標記

**美觀且功能完整的前端 UI:**
- React + TypeScript + Material-UI
- 不只是「能用」,而是「好看又好用」
- 展示生產等級的產品質感

---

✅ **技術深度驗證**

**Quarkus 手動 SDK(展示 auto-instrumentation 盲點):**
- Account Service 和 Currency Exchange Service 使用 Quarkus
- 證明手動 SDK 可以解決 auto-instrumentation 不支援的框架
- 重建團隊對 SDK instrumentation 的信心

**跨語言 trace propagation 無缺口:**
- Java SpringBoot → Python → Java Quarkus → Kafka → Python
- 完整 trace_id 傳遞,無任何斷點
- 展示「真正的完整可觀測性」

**自定義 span attributes 包含業務語義:**
- `account.id`, `account.currency`, `account.balance`
- `transaction.id`, `transaction.amount`, `transaction.type`
- `exchange.from_currency`, `exchange.to_currency`, `exchange.rate`
- 不只是技術指標,還有業務上下文

**真實故障場景(非刻意設計的 demo 錯誤):**
- Chaos Mesh 注入真實的網路延遲、Pod kill、CPU stress
- 不是「寫一個會拋錯的 function」,而是「模擬生產環境故障」
- 展示可觀測性在真實問題排查中的價值

---

### 關鍵成功定義總結

**對參與者(種子對象):**
- 看完一場震撼的技術電影
- 技術視野被打開,理解完整可觀測性的可能性
- 記住「有這個選項存在」,當時機成熟時想起

**對主人(技術傳道者):**
- 完整展示所有功能,沒有遺憾
- 複習並深化可觀測性知識(OpenTelemetry、SAGA、Chaos、Kafka)
- 追求技術完整性,模擬真實金融場景

**對技術社群:**
- 埋下完整可觀測性的種子
- 讓懂的人欣賞技術細節的完整性
- 影響力隨著人員流動自然擴散

**這不是商業產品,這是技術啟蒙。成功不在於有多少人導入,而在於有多少人的技術視野被打開。**

---

## MVP Scope & Implementation Phases

### Phase 1: Core Features (Must Have)

**定位:** Phase 1 完成標準 = 所有銀行功能正常運作 + log ↔ trace ↔ metric 三向跳轉完整展示

---

#### 1. 銀行核心功能

**帳戶管理:**
- ✅ 多幣別帳戶支援(台幣 TWD、美金 USD、日幣 JPY)
- ✅ 帳戶餘額查詢與管理
- ✅ 帳戶資料持久化(PostgreSQL)

**交易功能:**
- ✅ **存款/提款操作** - 單一帳戶的金額增減
- ✅ **轉帳功能(含 SAGA 完整流程)** - 跨帳戶轉帳,包含跨幣別轉帳
  - 完整 SAGA 編排:凍結 → 查匯率 → 扣款 → 入帳 → 記錄 → 通知
  - 補償邏輯(Compensating Transactions)完整實作
  - 同步處理(符合金融業務需求)
- ✅ **換匯功能** - 幣別兌換操作
  - 匯率查詢(模擬,不串接真實 API)
  - Exchange Rate Service 提供即時匯率
- ✅ **交易歷史查詢** - 完整的交易記錄與查詢功能

---

#### 2. 完整可觀測性實作

**OpenTelemetry 手動 SDK Instrumentation:**
- ✅ **SpringBoot 微服務** (API Gateway, Auth, User, Transaction, Deposit/Withdrawal)
  - 完整 trace context propagation
  - 自定義 span attributes(業務語義)
- ✅ **Quarkus 微服務** (Account, Currency Exchange)
  - 展示 auto-instrumentation 盲點的解決方案
  - 手動 SDK 完整實作
- ✅ **Python 微服務** (Transfer, Exchange Rate, Notification)
  - 跨語言 trace propagation
  - Kafka Consumer instrumentation

**log ↔ trace ↔ metric 三向跳轉:**
- ✅ **Grafana Tempo** - 分散式追蹤儲存與查詢
  - 從 trace 跳轉到 Loki logs
  - 從 trace 查看 span 詳細資訊
  - Service graph 視覺化
- ✅ **Grafana Loki** - 日誌聚合與查詢
  - 從 logs 跳轉回 trace(透過 trace_id)
  - 統一 trace context 關聯
- ✅ **Prometheus + Grafana** - 指標採集與視覺化
  - 從 metrics 異常跳轉到對應 trace
  - JVM metrics, HTTP metrics, 業務 metrics
- ✅ **OpenTelemetry Collector** - 統一資料收集與路由

**SAGA 流程完整追蹤:**
- ✅ Grafana Tempo 顯示轉帳 SAGA 的 6 個步驟
- ✅ 正常流程與失敗流程的 trace 視覺化
- ✅ 補償邏輯執行狀態在 trace 中清楚標記
- ✅ 每個步驟的執行時間與狀態追蹤

**Chaos Mesh 真實故障場景:**
- ✅ **NetworkChaos** - Exchange Rate Service 網路延遲 200ms
- ✅ **PodChaos (kill)** - Transfer Service Pod 突然死亡
- ✅ **PodChaos (failure)** - Transaction Service 容器失效
- ✅ **StressChaos (CPU)** - Account Service CPU 100%
- ✅ **StressChaos (Memory)** - Transaction Service OOM
- ✅ **NetworkPartition** - Account Service 與 Transaction Service 斷線
- ✅ 預定義 YAML 配置,即時觸發(`kubectl apply -f chaos-scenarios/`)

**前端到後端完整鏈路:**
- ✅ **React OpenTelemetry Browser SDK** - 前端 trace 生成
- ✅ 從使用者點擊按鈕到後端微服務的完整 trace
- ✅ HTTP request 自動帶入 `traceparent` header
- ✅ 前端錯誤與後端 trace 關聯

**Kafka 異步通訊追蹤:**
- ✅ Kafka Producer trace context propagation
- ✅ Kafka Consumer 繼承 trace_id
- ✅ 跨訊息佇列的完整鏈路視覺化
- ✅ Notification Service 異步處理追蹤

---

#### 3. 前端 UI 完整實作

**技術棧:**
- ✅ React + TypeScript + Material-UI
- ✅ OpenTelemetry Browser SDK
- ✅ WebSocket Client(接收推播通知)
- ✅ Axios HTTP Client(自動注入 trace context)

**功能頁面:**
- ✅ **登入頁** - 簡化認證(基本 JWT,無需完整權限管理)
- ✅ **儀表板(Dashboard)** - 帳戶總覽 + **圖表視覺化**
  - 多幣別帳戶餘額顯示
  - 帳戶餘額趨勢圖表(Line Chart)
  - 交易統計圖表(Bar Chart / Pie Chart)
  - 視覺化設計展示生產等級質感
- ✅ **轉帳頁** - 來源/目標帳戶選擇、金額輸入、即時匯率顯示
- ✅ **換匯頁** - 幣別選擇、即時匯率顯示、換匯操作
- ✅ **存提款頁** - 帳戶選擇、操作類型(存款/提款)、金額輸入
- ✅ **交易歷史頁** - 交易記錄列表、篩選、排序

**WebSocket 推播通知(創新功能):**
- ✅ Notification Service 透過 WebSocket 推送通知到前端
- ✅ Material-UI Snackbar/Toast 顯示推播彈窗
- ✅ 通知內容:「轉帳成功!已從帳戶 TWD-001 轉出 1000 元到 USD-002」
- ✅ **展示 Kafka 異步通訊的完整鏈路追蹤**:
  ```
  使用者點擊「轉帳」
    ↓ (Browser 生成 trace_id)
  Transfer Service 完成轉帳
    ↓ (發送 Kafka 訊息,帶 trace context)
  Notification Service 消費訊息
    ↓ (WebSocket 推送,繼承 trace_id)
  React UI 彈窗顯示通知
    ↓
  完整 trace:前端點擊 → 後端處理 → Kafka → WebSocket → 前端彈窗
  ```

**UI/UX 設計標準:**
- ✅ Material-UI 元件庫,美觀且一致的設計風格
- ✅ 不只是「能用」,而是「好看又好用」
- ✅ 展示生產等級的產品質感
- ✅ Responsive 設計(桌面優先)

---

#### 4. 技術架構完整實作

**10 個微服務架構:**

```yaml
banking-services namespace:
  1. API Gateway (SpringBoot)        - 統一入口,trace context 注入起點
  2. Auth Service (SpringBoot)       - JWT 認證與授權
  3. User Service (SpringBoot)       - 使用者資料管理
  4. Account Service (Quarkus) ⭐    - 多幣別帳戶、餘額管理
  5. Transaction Service (SpringBoot) - 交易記錄與歷史查詢
  6. Transfer Service (Python) ⭐     - SAGA 編排器,轉帳流程編排
  7. Deposit/Withdrawal Service (SpringBoot) - 存提款操作
  8. Exchange Rate Service (Python)  - 即時匯率查詢(模擬)
  9. Currency Exchange Service (Quarkus) ⭐ - 換匯業務編排
 10. Notification Service (Python)   - 異步通知處理 + WebSocket 推送

observability namespace:
  - Grafana
  - Tempo
  - Loki
  - Prometheus
  - OpenTelemetry Collector

messaging namespace:
  - Kafka (KRaft mode,無 ZooKeeper)

chaos namespace:
  - Chaos Mesh
```

**Kubernetes 部署:**
- ✅ 多 Namespace 隔離設計
- ✅ Service Discovery(K8s DNS)
- ✅ ConfigMap / Secret 管理
- ✅ **完整部署腳本**(Helm Charts 或 Kustomize 或 Shell scripts)
  - 一鍵部署所有服務
  - 環境變數配置
  - 資料庫初始化腳本

**資料持久化:**
- ✅ **PostgreSQL** - 真實關聯式資料庫
  - 帳戶資料(accounts)
  - 使用者資料(users)
  - 交易記錄(transactions)
  - 資料持久化(重啟後資料不消失)
- ✅ 資料庫初始化腳本(測試帳戶、初始餘額)

**Kafka KRaft Mode:**
- ✅ Kafka 4.0+ 原生 KRaft 模式
- ✅ 無需 ZooKeeper,簡化架構
- ✅ Topic: `banking.notifications`
- ✅ Producer: Transfer Service
- ✅ Consumer: Notification Service

---

#### 5. 錯誤處理與補償邏輯

**Level 2 錯誤分類處理(符合 Phase 1 標準):**

**業務錯誤(可預期):**
- ✅ `InsufficientBalanceException` - 餘額不足
  - 錯誤訊息:「餘額不足,無法完成轉帳」
  - Trace span 標記:`error.type=InsufficientBalance`
- ✅ `AccountNotFoundException` - 帳戶不存在
  - 錯誤訊息:「帳戶不存在」
  - Trace span 標記:`error.type=AccountNotFound`
- ✅ `InvalidAmountException` - 金額無效(負數或零)
  - 錯誤訊息:「金額無效」
  - Trace span 標記:`error.type=InvalidAmount`

**外部依賴錯誤:**
- ✅ `ExchangeRateTimeoutException` - 匯率查詢超時
  - 錯誤訊息:「匯率查詢失敗,請稍後重試」
  - 觸發補償邏輯(解凍帳戶)
  - Trace span 標記:`error.type=ExchangeRateTimeout`
  - **Chaos Mesh 可觸發此場景**(NetworkChaos 延遲 200ms)

**系統錯誤(未預期):**
- ✅ `Exception` - 其他未分類錯誤
  - 錯誤訊息:「系統錯誤,轉帳失敗」
  - 觸發補償邏輯
  - Trace span 標記:`error.type=SystemError`

**補償邏輯(Compensating Transactions):**
```java
SAGA 步驟與補償:
1. 凍結來源帳戶    → 失敗:直接返回錯誤
2. 查詢匯率        → 失敗:解凍來源帳戶
3. 扣除來源帳戶    → 失敗:解凍來源帳戶
4. 增加目標帳戶    → 失敗:退回來源帳戶 + 解凍
5. 記錄交易        → 失敗:回滾所有帳戶操作
6. 發送通知(Kafka) → 失敗:僅記錄錯誤(不影響交易完成)
```

**錯誤在 Trace 中的可視化:**
- ✅ 錯誤 span 自動標記為紅色
- ✅ Span attributes 包含錯誤類型、錯誤訊息、Stack trace
- ✅ 補償邏輯執行的 span 清楚標記
- ✅ Demo 時可以展示不同錯誤場景的 trace

---

#### 6. 技術完整性標準

**金融交易真實性:**
- ✅ 轉帳/換匯採用**同步處理**(非異步)
- ✅ 使用者在 UI 點擊後立即知道成功或失敗
- ✅ Kafka 異步**僅用於通知服務**(簡訊、Email、推播)
- ✅ 符合金融系統的實際設計模式

**跨語言 trace propagation 無缺口:**
- ✅ Java SpringBoot → Python → Java Quarkus → Kafka → Python
- ✅ 完整 trace_id 傳遞,無任何斷點
- ✅ 展示「真正的完整可觀測性」

**自定義 span attributes 包含業務語義:**
- ✅ `account.id`, `account.currency`, `account.balance`
- ✅ `transaction.id`, `transaction.amount`, `transaction.type`
- ✅ `exchange.from_currency`, `exchange.to_currency`, `exchange.rate`
- ✅ 不只是技術指標,還有業務上下文

**真實故障場景(非刻意設計的 demo 錯誤):**
- ✅ Chaos Mesh 注入真實的網路延遲、Pod kill、CPU stress
- ✅ 不是「寫一個會拋錯的 function」
- ✅ 模擬生產環境故障

---

### Out of Scope for Phase 1 (明確不做)

**多租戶(Multi-tenancy):**
- ❌ 不實作多租戶架構
- ✅ 採用單租戶設計(所有使用者在同一個「銀行」下)
- **理由**: 簡化架構,專注於可觀測性展示

**真實外部 API 串接:**
- ❌ 不串接真實匯率 API
- ✅ Exchange Rate Service 提供模擬匯率
- **理由**: 避免外部依賴,Demo 環境穩定性優先

**真實簡訊/Email 發送:**
- ❌ 不整合真實簡訊/Email 服務(如 Twilio, SendGrid)
- ✅ 採用 WebSocket 推播通知取代
- **理由**: WebSocket 推播更適合 Demo 展示,即時視覺化效果更好

**完整使用者權限管理:**
- ❌ 不實作完整的 RBAC(角色權限控制)
- ✅ 簡化認證機制(基本 JWT 即可)
- **理由**: 非可觀測性展示重點

**Level 3 錯誤處理(進階機制):**
- ❌ 不實作重試機制(Retry with exponential backoff)
- ❌ 不實作斷路器(Circuit Breaker)
- ❌ 不實作降級策略(Fallback)
- ❌ 不建立詳細錯誤碼系統(ERR_001, ERR_002...)
- ✅ Level 2 錯誤分類處理已足夠
- **理由**: 增加複雜度,Phase 1 聚焦核心可觀測性

**效能測試/負載測試:**
- ❌ 不進行效能測試、負載測試、壓力測試
- ✅ 基本微服務架構應可承受 Demo 演示負載
- **理由**: 非 Phase 1 重點,架構設計已考慮擴展性

**完整文件:**
- ❌ 不撰寫完整的架構文件、API 文件、開發指南
- ✅ 部署腳本完整,包含必要的 README
- **理由**: 文件可延後,優先確保 Demo 功能完整

---

### MVP Success Criteria (Phase 1 完成標準)

**功能完整性檢查清單:**

✅ **所有銀行功能正常運作:**
- [ ] 多幣別帳戶管理(台幣、美金、日幣)
- [ ] 存款/提款操作
- [ ] 轉帳功能(含跨幣別轉帳)
- [ ] 換匯功能
- [ ] 交易歷史查詢

✅ **log ↔ trace ↔ metric 三向跳轉完整展示:**
- [ ] 從 Grafana Tempo trace 跳轉到 Loki logs
- [ ] 從 Loki logs 跳轉回 trace(透過 trace_id)
- [ ] 從 Prometheus metrics 跳轉到對應 trace
- [ ] 統一 trace context 自動關聯

✅ **SAGA 流程完整追蹤:**
- [ ] 正常流程在 Grafana Tempo 清楚顯示
- [ ] 失敗流程的補償邏輯可視化
- [ ] 每個步驟的執行狀態和耗時追蹤

✅ **Chaos Mesh 真實故障場景:**
- [ ] 至少 3 種故障場景可即時觸發
- [ ] Trace 自動顯示故障發生位置
- [ ] 展示可觀測性在真實故障下的價值

✅ **前端到後端完整鏈路:**
- [ ] React UI 點擊按鈕生成 trace
- [ ] 完整 trace 串起前端 → 後端 → Kafka → WebSocket
- [ ] WebSocket 推播通知顯示在 UI

✅ **部署腳本完整:**
- [ ] 一鍵部署所有服務到 K8s
- [ ] 環境變數配置清晰
- [ ] 資料庫初始化腳本可用

**Demo 演示驗證:**
- [ ] 可以順利執行完整 Demo 流程(15-20 分鐘)
- [ ] 所有核心功能無嚴重 Bug
- [ ] Grafana 視覺化效果良好
- [ ] UI 美觀且操作流暢

---

### Future Vision: Phase 2 & Beyond

**Phase 2: AI SRE 與進階功能**

✨ **AI SRE 根因分析(殺手級功能):**
- 🔮 LLM 自動從 Tempo + Loki + Prometheus 取得資料
- 🔮 產生根因分析報告和建議解決方案
- 🔮 展示「AI 時代的可觀測性未來」
- 🔮 支援 GPT-4o / Claude 3.5 Sonnet / 本地模型

✨ **完整文件:**
- 🔮 架構設計文件(Architecture Decision Records)
- 🔮 API 文件(OpenAPI / Swagger)
- 🔮 部署指南(詳細步驟、故障排查)
- 🔮 開發指南(如何新增微服務、如何擴展功能)

✨ **Level 3 錯誤處理:**
- 🔮 重試機制(Retry with exponential backoff)
- 🔮 斷路器(Circuit Breaker with Resilience4j)
- 🔮 降級策略(Fallback)
- 🔮 詳細錯誤碼系統

✨ **真實 API 整合:**
- 🔮 真實匯率 API 串接(如 Open Exchange Rates API)
- 🔮 展示外部 API 整合的可觀測性

**Phase 3: 生產就緒增強**

✨ **效能優化:**
- 🔮 效能測試(JMeter / Gatling)
- 🔮 負載測試與調優
- 🔮 快取策略(Redis)
- 🔮 資料庫查詢優化

✨ **安全強化:**
- 🔮 完整 JWT 認證與授權
- 🔮 RBAC 角色權限控制
- 🔮 API Rate Limiting
- 🔮 資料加密(傳輸 + 儲存)

✨ **可觀測性進階:**
- 🔮 更多 Chaos 場景(網路分區、時鐘偏移、磁碟故障)
- 🔮 Service Mesh 整合(Istio / Linkerd)
- 🔮 分散式追蹤進階功能(Tail Sampling, Span Metrics)

---

### 技術債務管理(Phase 1 已知限制)

**已知簡化設計(Phase 1 可接受):**
- ⚠️ 簡化認證機制(無完整 RBAC)
- ⚠️ 模擬匯率查詢(非真實 API)
- ⚠️ Level 2 錯誤處理(無重試、斷路器)
- ⚠️ 無效能測試數據

**Phase 2 必須改善:**
- 🔧 認證機制完整化
- 🔧 錯誤處理機制強化
- 🔧 效能優化與測試

**不影響 Demo 展示的限制:**
- ✅ 這些簡化不影響可觀測性核心價值展示
- ✅ Phase 1 聚焦於「技術啟蒙」,非生產部署
- ✅ 技術債務已明確標記,Phase 2 可系統性改善

---

### 關鍵里程碑時間軸(參考)

**注意:** 這不是強制時間表,僅供參考規劃。

**Phase 1 主要任務分解:**
1. **Week 1-2**: 基礎架構搭建
   - K8s 環境建置
   - 資料庫設計與初始化
   - 微服務基本架構(10 個服務骨架)

2. **Week 3-4**: 核心業務功能
   - 帳戶管理功能
   - 存提款、轉帳、換匯功能
   - SAGA 流程實作

3. **Week 5-6**: 可觀測性整合
   - OpenTelemetry SDK 整合(SpringBoot + Quarkus + Python)
   - Grafana Stack 部署
   - log ↔ trace ↔ metric 三向跳轉

4. **Week 7-8**: 前端 UI 與 Chaos
   - React UI 實作(所有功能頁面)
   - WebSocket 推播通知
   - Dashboard 圖表視覺化
   - Chaos Mesh 場景設計

5. **Week 9-10**: 整合測試與優化
   - 端到端測試
   - Bug 修復
   - 部署腳本完善
   - Demo 流程演練

**Phase 2 啟動時機:**
- Phase 1 完成並成功 Demo 後
- 或根據實際需求和資源調整
