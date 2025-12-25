---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
inputDocuments: ['/Users/cfh00592178/Documents/lite-bank-demo/docs/analysis/product-brief-lite-bank-demo-2025-12-04.md']
workflowType: 'prd'
lastStep: 11
project_name: 'lite-bank-demo'
user_name: '主人'
date: '2025-12-11'
completionDate: '2025-12-15'
---

# Product Requirements Document - lite-bank-demo

**Author:** 主人
**Date:** 2025-12-11

---

## Executive Summary

**lite-bank-demo** 是一個技術展示與教學型專案,旨在透過完整的可觀測性三本柱實作(log ↔ trace ↔ metric),展示現代微服務系統如何從傳統的 10-15 分鐘人工排查,提升到 1-2 分鐘自動化定位的效率躍升。本專案模擬真實銀行業務場景(多幣別帳戶、轉帳、換匯),結合 SAGA 分散式事務模式、Chaos Mesh 混沌工程,以及從前端到後端的完整 trace propagation,為團隊展示完整可觀測性的決定性價值。

### 核心問題與解決方案

**問題:** SRE 和開發團隊在排查微服務系統問題時,依賴手動拼湊 LOG、Metrics 和服務調用順序,平均需要 10-15 分鐘才能定位根因。更關鍵的是,團隊之前嘗試導入 OpenTelemetry auto-instrumentation 失敗(因 Quarkus 不支援),導致對可觀測性投資失去信心。

**解決方案:** 透過完整的手動 SDK instrumentation(包含 Quarkus)、SAGA 模式、跨語言 trace propagation(Java SpringBoot + Quarkus + Python)、異步通訊追蹤(Kafka KRaft)、以及前端 OpenTelemetry,展示可觀測性三本柱的完整威力,重建團隊對 SDK instrumentation 的信心。

### 目標使用者

**主要使用者:** 初階 SRE 與開發人員(約 1 年工作經驗)
- 僅有基本微服務概念,未深入實作過分散式系統
- 已使用 Grafana,能建立簡單 Dashboard
- 對 OpenTelemetry、SAGA、Chaos Engineering 僅有名詞了解
- 對 auto-instrumentation 失敗經驗心有餘悸

**使用情境:** 公司內部技術分享會(workshop 形式,無後續支援機制)
**關鍵限制:** 使用者本人無導入決策權,存在政治性阻礙
**真正目標:** 技術啟蒙而非立即導入 - 埋下種子,當重大事件發生或環境改變時想起這個方案

### What Makes This Special

**1. 完整解決 Auto-instrumentation 盲點**
- 手動 SDK 確保所有服務(包含 Quarkus)完整追蹤,零鏈路缺口
- 自定義 span attributes 包含業務語義(帳戶 ID、交易金額、幣別)
- 重建團隊對 SDK instrumentation 的信心

**2. SAGA 分散式事務可視化(殺手級場景)**
- Grafana Tempo 清楚顯示轉帳 SAGA 的 6 個步驟
- 失敗時可看到補償邏輯(Compensating Transactions)執行過程
- 這是完整 trace 的最佳展示場景

**3. Chaos Engineering 即時整合(真實故障模擬)**
- Chaos Mesh 真實注入 NetworkChaos、PodChaos、StressChaos
- Workshop 現場觸發故障 → 1-2 分鐘內在 Tempo 定位根因
- 展示「10-15 分鐘手動排查 vs 1-2 分鐘自動定位」的決定性對比

**4. 完整 Trace Propagation (前端到 WebSocket)**
- React OpenTelemetry Browser SDK → API Gateway → 微服務 → Kafka → WebSocket
- 一條 trace_id 串起整個系統,無斷點
- 展示跨語言(Java + Python)、跨協議(REST + Kafka + WebSocket)完整追蹤

---

## Project Classification

### 專案類型 (Project Type)

**檢測到的類型:** `web_app` + `developer_tool` 混合型

**類型組成:**
- **60% Developer Tool 屬性:** 技術展示、教學工具、Workshop 素材
- **40% Web App 屬性:** 完整前端 UI、使用者互動、即時更新

**核心定位:** 技術啟蒙工具,而非生產級應用程式

### 領域 (Domain)

**主要領域:** Fintech (金融科技)
- 模擬真實銀行業務場景(多幣別帳戶、轉帳、換匯)
- 展示金融系統核心模式(SAGA、Idempotency、Audit Trail)
- 不需要真實金融牌照或完整合規認證

**次要領域:** DevOps / SRE (可觀測性展示)
- 完整可觀測性三本柱整合(Tempo + Loki + Prometheus)
- Chaos Engineering 實踐
- 分散式系統除錯技術

### 複雜度等級 (Complexity Level)

**整體複雜度:** High (高複雜度)

**複雜度來源:**
- 分散式交易(SAGA Pattern)
- 跨語言 trace propagation(Java + Python + Quarkus + React)
- 異步通訊追蹤(Kafka + WebSocket)
- Chaos Engineering 整合
- 前端到後端完整鏈路可觀測性

**簡化策略(Demo 級別):**
- 不實作真實金融合規功能(KYC/AML)
- 簡化認證機制(基本 JWT,無 refresh token)
- 不實作詐欺偵測、風險評分
- 模擬匯率查詢(非真實第三方 API)

### 技術棧 (Tech Stack)

**前端:**
- React 18+ with TypeScript
- React Router v6 (SPA)
- Material-UI / Ant Design
- OpenTelemetry Browser SDK
- WebSocket Client

**後端:**
- Java SpringBoot 3.x (Account Service, Payment Service, Transaction Service)
- Java Quarkus 3.x (Exchange Service,展示手動 SDK instrumentation)
- Python FastAPI (可選)
- Kafka KRaft mode (異步通訊,無 ZooKeeper)
- PostgreSQL (資料庫)

**可觀測性:**
- Grafana Tempo (Distributed Tracing)
- Grafana Loki (Log Aggregation)
- Prometheus (Metrics)
- Grafana (Dashboard)
- OpenTelemetry SDK (手動 instrumentation,跨所有服務)

**混沌工程:**
- Chaos Mesh (Kubernetes Chaos Engineering)

**基礎設施:**
- Kubernetes (minikube/kind 本地 或 GKE/EKS 雲端)
- Helm Charts (部署自動化)
- Docker Compose (本地快速啟動)

### 開發階段 (Development Stage)

**當前階段:** Phase 1 - MVP (技術展示核心功能)

**Phase 1 目標(必須完成):**
- 所有銀行功能正常(存提款、轉帳、換匯、交易歷史)
- SAGA 補償邏輯完整
- OpenTelemetry 完整 trace propagation(前端到 WebSocket)
- Grafana Tempo SAGA 可視化
- Chaos Mesh 故障注入與即時定位
- log ↔ trace ↔ metric 三向跳轉

**Phase 2 探索(加分項):**
- AI SRE 根因自動分析(LLM 整合)
- 金融合規增強(KYC/AML 架構預留)
- 效能優化與測試

**Phase 3 願景(未來考慮):**
- 完整生產級別安全性(欄位加密、KMS)
- 詐欺偵測引擎
- 多租戶支援

### 時程與資源

**預估時程:** 8-12 週(Phase 1 MVP)

**團隊組成建議:**
- 1 位資深後端工程師(熟悉 SAGA、OpenTelemetry、Kafka)
- 1 位前端工程師(React、OpenTelemetry Browser SDK)
- 1 位 SRE/DevOps(Kubernetes、Grafana Stack、Chaos Mesh)
- 可選:1 位技術文件工程師(教學文件、Workshop 腳本)

**關鍵里程碑:**
- Week 4:後端微服務 + SAGA 完成,OpenTelemetry 手動 SDK 整合完成
- Week 6:前端 UI + WebSocket 整合完成,完整 trace propagation 驗證
- Week 8:Chaos Mesh 整合完成,三向跳轉功能正常
- Week 10:部署腳本(Helm + Docker Compose)、教學文件完成
- Week 12:Workshop 腳本完成,完整 Demo 流程驗證

**技術債務管理(Phase 1 已知限制,可接受):**
- 簡化認證機制(無完整 RBAC)
- 模擬匯率查詢(非真實 API)
- Level 2 錯誤處理(無重試、斷路器)
- 無效能測試數據

**這些簡化不影響 Demo 展示的核心價值,Phase 1 聚焦於「技術啟蒙」而非生產部署。**

---

## User Journeys

### Journey 1: Alex Chen - 從無助到頓悟的技術啟蒙之夜

**Opening Scene: 深夜排查的無力感 (The Late-Night Struggle)**

凌晨 2 點,Alex 盯著三個螢幕:左邊是 Grafana Dashboard 顯示 API 延遲突然飆高,中間是 Kibana 顯示一堆錯誤 LOG,右邊是服務架構圖。他需要手動拼湊:「payment-service 在 23:47:32 拋出 TimeoutException...然後 account-service 在 23:47:34 記錄 BalanceUpdateFailed...再去查 exchange-service 的 LOG...」15 分鐘過去,他終於找到根因是 exchange-service 調用第三方 API 超時,但凌晨 2:17 了。

第二天早上,Alex 收到 workshop 邀請:「完整可觀測性:從 10-15 分鐘排查到 1-2 分鐘定位」。他想起昨晚的折磨,心想「又是那種自動化夢想,上次 auto-instrumentation 根本沒用...」但還是報名了,畢竟昨晚真的太痛苦。

**Rising Action: 從懷疑到震撼 (From Skepticism to Shock)**

Workshop 開始,主講者展示一個銀行轉帳系統,Alex 心想「又是那種完美 demo...」。但當主講者說「我們現在用 Chaos Mesh 注入真實網路延遲」並即時按下按鈕,Alex 看到:

1. **即時觸發:** 主講者在 UI 上點擊「注入 NetworkChaos:exchange-service 延遲 5 秒」
2. **立即執行轉帳:** 前端轉帳頁面點擊「轉帳 USD 100」,畫面顯示處理中...
3. **1 分鐘後定位根因:** 主講者打開 Grafana Tempo,複製 trace_id,立即看到:
   - 完整 SAGA 流程的 6 個步驟可視化
   - exchange-service 的 span 顯示 5.2 秒延遲(紅色標記)
   - 補償邏輯自動執行(unfreeze balance → rollback transaction)
4. **三向跳轉展示:** 從 trace 點擊跳到 Loki logs,看到詳細錯誤訊息和 stack trace;從 logs 跳回 trace;從 Prometheus metrics 跳到對應 trace

Alex 愣住了。這不是「完美 demo」,這是**真實故障注入後的即時定位**。他想起昨晚的 15 分鐘手動拼湊,如果有這個工具...

**Climax: The "Aha! Moment" - 技術視野的打開**

主講者繼續展示:「這是前端到後端的完整 trace。」Alex 看到 Grafana Tempo 顯示:

```
[React Browser SDK] → [API Gateway] → [Transfer Service]
→ [Account Service] → [Transaction Service] → [PostgreSQL]
```

一條 trace_id 串起 7 個微服務，全部使用 Java SpringBoot，沒有斷點。Alex 腦中響起聲音:「原來可觀測性可以做到這樣...原來 SAGA 可以這樣 debug...原來我們之前失敗是因為沒用對方法...」

主講者說:「這是手動 SDK instrumentation,不是 auto-instrumentation。手動 SDK 能提供更精確的業務語義標註。」Alex 突然理解:不是可觀測性沒用,是我們用錯工具了。

**Resolution: 種子埋下,等待發芽 (The Seed is Planted)**

Workshop 結束,Alex 回到座位,寫下筆記:「10-15 分鐘 → 1-2 分鐘,手動 SDK,SAGA 可視化,Chaos Mesh 真實故障,log ↔ trace ↔ metric 三向跳轉。」他知道短期內無法導入(政治阻礙、預算限制),但種子已經埋下。

3 個月後,生產環境發生重大事件,整個團隊花了 2 小時排查分散式事務問題。會議室裡,Alex 說:「我記得那個 workshop...如果我們有那個可觀測性方案,可能 10 分鐘就定位了。」主管皺眉:「什麼 workshop?」Alex 開始描述 SAGA 可視化、Chaos 真實故障、完整 trace 鏈路...

種子發芽了。雖然當下還沒導入,但「有這種完整方案存在」這個認知,已經刻進 Alex 的技術視野。當環境改變、主管更替、或下一次重大事件發生時,這個種子會再次發芽。

---

### Journey 2: Sarah Lin - 從懷疑到認同的架構評估之旅

**Opening Scene: 技術 Lead 的謹慎評估 (The Cautious Evaluation)**

Sarah 是資深技術 Lead,8 年分散式系統經驗,看過太多「完美 demo」在生產環境翻車。她參加這場 workshop 是因為老闆要求「評估可觀測性投資」,但她心裡清楚:上次 OpenTelemetry auto-instrumentation 導入失敗後,管理層對這類投資已失去耐心。

Workshop 開始,Sarah 不像其他人驚嘆,她在筆記本上列檢查清單:
- 跨語言 trace propagation 是否完整?
- Kafka 異步通訊如何追蹤?
- 補償邏輯(Compensating Transactions)是否可見?
- 自定義 span attributes 是否包含業務語義?
- Chaos 是真實故障還是刻意設計的錯誤?
- 部署複雜度如何?
- 生產環境可行性?

**Rising Action: 逐項驗證的技術審查 (Methodical Technical Audit)**

主講者展示 SAGA 流程,Sarah 仔細觀察 Grafana Tempo:

1. **技術完整性驗證:**
   - SpringBoot → Quarkus:trace_id 完整傳遞(她之前以為 Quarkus 不支援)
   - 同步調用 → Kafka 異步:看到 Kafka producer span 和 consumer span 正確關聯
   - 自定義 attributes:account.id, transaction.amount, exchange.rate 等業務資訊清楚可見
   - 補償邏輯:失敗時看到 unfreeze balance → rollback transaction 的完整執行過程

2. **真實性驗證:**
   - 主講者用 Chaos Mesh 注入 NetworkChaos,不是程式碼裡刻意寫的錯誤
   - Chaos 觸發後,trace 立即反映真實延遲(5.2 秒),不是假資料
   - 錯誤處理是真實的 SAGA 補償邏輯,不是 try-catch

3. **生產可行性評估:**
   - 部署腳本完整(Helm/Kustomize),不是「講師電腦才能跑」
   - Kafka KRaft mode(無 ZooKeeper),符合現代架構
   - 前端到後端完整鏈路,符合真實業務場景

Sarah 在筆記本上打勾,她意識到:這不是「完美 demo」,這是**生產等級的架構範例**。

**Climax: The Recognition - 技術 Lead 的認同**

當主講者展示「log ↔ trace ↔ metric 三向跳轉」時,Sarah 看到:

- 從 Grafana Tempo trace 點擊跳到 Loki logs
- 從 Loki logs 的 trace_id 連結跳回 Tempo
- 從 Prometheus metrics 的 exemplar 跳到對應 trace

Sarah 腦中開始計算 ROI:「如果我們團隊平均每週處理 5 次生產事件,每次排查平均 15 分鐘,有了這個方案可以降到 2 分鐘...每週省 65 分鐘,一年省 56 小時...這還不包括減少誤判和改善 MTTR 的價值...」

她突然理解:這不是「技術炫技」,這是**真實的生產力提升**。更重要的是,這證明了「手動 SDK instrumentation 可以解決我們之前 auto-instrumentation 失敗的問題」。

**Resolution: 等待時機的推動者 (The Advocate in Waiting)**

Workshop 結束,Sarah 沒有立即推動導入(她知道政治環境不允許),但她做了三件事:

1. **技術驗證完成:** 在筆記本上寫下「技術可行,生產等級,ROI 明確」
2. **種子埋下:** 記住「當下次重大事件發生時,這是解決方案」
3. **準備說服材料:** 如果管理層問「為什麼需要可觀測性投資?」,她可以描述這個完整方案的價值

6 個月後,生產環境發生重大資料不一致問題,整個團隊花了 4 小時排查分散式事務。事後檢討會議上,CTO 問:「怎麼避免下次再發生?」

Sarah 說:「我們需要完整的分散式追蹤。我之前看過一個方案,可以讓 SAGA 補償邏輯完全可見,排查時間從小時級降到分鐘級。而且這個方案解決了我們之前 auto-instrumentation 失敗的問題,因為它用手動 SDK...」

種子發芽了。雖然當下還沒確定預算,但 Sarah 已經成為推動者。當組織環境改變、預算釋出、或下一次重大事件發生時,這個方案會被正式提案。

---

### Journey Requirements Summary

**從兩個使用者旅程中揭示的產品能力需求:**

**1. 視覺化衝擊力(Alex 的核心需求):**
- 10-15 分鐘 vs 1-2 分鐘的明顯對比展示
- SAGA 流程完整可視化(6 個步驟清楚呈現)
- 補償邏輯執行過程可見
- 前端到後端完整 trace 鏈路無斷點

**2. 真實性驗證(Sarah 的核心需求):**
- Chaos Mesh 真實故障注入(非刻意設計的錯誤)
- 跨語言 trace propagation 完整(SpringBoot → Python → Quarkus)
- Kafka 異步通訊追蹤正確
- 自定義 span attributes 包含業務語義

**3. 生產可行性(Sarah 的評估標準):**
- 部署腳本完整(一鍵部署)
- 現代架構(Kafka KRaft、K8s 多 Namespace)
- 錯誤處理與補償邏輯完整
- UI 美觀且功能完整(展示生產等級質感)

**4. 技術啟蒙效果(兩者共同需求):**
- 重建對手動 SDK instrumentation 的信心
- 理解完整可觀測性的價值(log ↔ trace ↔ metric 三本柱)
- 記住關鍵數字和概念,當未來遇到困難時能想起
- 種子埋下,等待發芽(長期影響力)

---

## Domain-Specific Requirements (Fintech 金融科技領域)

### Fintech Compliance & Regulatory Overview

**lite-bank-demo** 作為金融科技領域的技術展示專案,雖然不需要真實金融牌照或完整合規認證,但在架構設計與實作上必須展示「生產等級思維」,確保觀眾(特別是資深技術 Lead)能認同這是可延伸至真實生產環境的參考架構。

**Domain Context:**
- **領域定位:** Fintech (金融科技) - 模擬真實銀行業務場景
- **複雜度等級:** High (高複雜度) - 涉及分散式交易、資料一致性、稽核軌跡
- **合規策略:** Demo 級別實作 + 生產級別架構思維 + 明確標註技術債務

### Key Domain Concerns (關鍵領域考量)

#### 1. Regional Compliance (區域合規) - Phase 2 考量

**Demo 階段處理方式:**
- **不實作** 真實 KYC/AML 流程(認識客戶/反洗錢)
- **架構預留** 擴展點:帳戶開戶時預留身份驗證 hook、交易時預留風險評分檢查點
- **文件標註** 生產環境需考慮:台灣金管會規範、GDPR(歐盟)、CCPA(美國加州)

**生產級別考量(標註於架構文件):**
- KYC 流程整合點:User Service 帳戶開戶流程
- AML 交易監控整合點:Transaction Service 交易記錄後的風險評分 hook
- 合規報表生成:預留 Reporting Service 介面

#### 2. Security Standards (安全標準) - 部分實作

**JWT 認證機制(簡化版):**
- **實作範圍:** 基本 JWT token 生成與驗證(含 user_id, account_id claims)
- **API Gateway 層級驗證:** 所有請求需有效 JWT token
- **技術債務:** 無 refresh token 機制、無完整 token revocation、簡化過期時間管理

**敏感資料處理(Demo 等級):**
- **帳戶餘額、交易金額:** 明文儲存於資料庫(標註為技術債務)
- **密碼處理:** 使用 bcrypt/Argon2 hash(展示基本安全意識)
- **不實作:** 欄位層級加密、Key Management System(KMS)

**API 安全性(基本防護):**
- **不實作** API Rate Limiting(Phase 2 考量)
- **不實作** API Key 管理(Phase 2 考量)
- **實作** HTTPS/TLS 連線(K8s Ingress 層級)

**生產級別考量(Phase 2/3):**
- PCI DSS Level 1 合規要求(若處理真實信用卡資料)
- 敏感欄位 AES-256 加密 + KMS 整合
- API Rate Limiting(防 DDoS)
- WAF(Web Application Firewall)整合

#### 3. Audit Requirements (審計需求) - **核心展示重點**

**交易記錄不可篡改特性(生產級別實作):**
- **Append-Only Transaction Table:** 交易記錄僅能新增,不可修改/刪除
- **完整稽核軌跡:** 每筆交易記錄包含:
  - `transaction_id` (UUID)
  - `trace_id` (OpenTelemetry trace ID,關鍵整合點!)
  - `account_id_from` / `account_id_to`
  - `amount` / `currency`
  - `transaction_type` (DEPOSIT, WITHDRAWAL, TRANSFER, EXCHANGE)
  - `status` (PENDING, COMPLETED, FAILED, COMPENSATED)
  - `created_at` (timestamp,不可變)
  - `metadata` (JSON,包含 SAGA 補償邏輯執行記錄)

**完整交易歷史查詢(展示可觀測性整合):**
- **功能實作:** Transaction History 頁面,可查詢指定帳戶的所有交易記錄
- **trace_id 整合:** 每筆交易記錄顯示對應的 `trace_id`,點擊可跳轉至 Grafana Tempo 完整 trace
- **SAGA 狀態可見:** 顯示交易是否觸發補償邏輯(COMPENSATED 狀態)
- **時間範圍查詢:** 支援依日期範圍篩選交易記錄

**不實作(Out of Scope):**
- 合規報表生成(如:月度交易統計、異常交易報告)
- 外部稽核系統整合(如:稽核日誌匯出至 SIEM)

**生產級別考量(Phase 2/3):**
- 交易記錄區塊鏈化(Blockchain/Distributed Ledger)
- 完整稽核日誌(Audit Log)記錄所有系統操作
- 合規報表自動生成(監管機構要求)

#### 4. Fraud Prevention (詐欺防範) - Out of Scope

**Demo 階段處理方式:**
- **不實作** 詐欺偵測邏輯(異常金額警告、高頻交易限制、IP 地理位置檢查)
- **不實作** 機器學習詐欺模型
- **聚焦於可觀測性:** 透過完整 trace 追蹤,展示「如果發生異常交易,如何快速定位」

**生產級別考量(Phase 3):**
- 即時詐欺偵測引擎(Rule-based + ML model)
- 異常交易自動凍結機制
- 使用者行為分析(UBA)

#### 5. Data Protection (資料保護) - Demo 等級

**資料庫層級保護(Level 2 實作):**
- **基本存取控制:** PostgreSQL user/password,應用程式層級 connection pool
- **Append-Only Transaction Table:** 交易記錄不可修改/刪除
- **技術債務:** 敏感欄位明文儲存(帳戶餘額、交易金額)

**隱私權考量(Demo 不涉及真實個資):**
- **測試資料:** 使用虛構使用者資料(Alex Chen, Sarah Lin 等)
- **不實作** GDPR 使用者資料刪除機制(Right to be Forgotten)
- **不實作** 資料遮罩(Data Masking)機制

**生產級別考量(Phase 2/3):**
- 敏感欄位 AES-256 加密(帳戶餘額、個人資料)
- 資料庫連線 TLS 加密
- Key Management System(KMS)整合
- GDPR/CCPA 合規:資料可攜權(Data Portability)、被遺忘權(Right to be Forgotten)

---

### Compliance Requirements (合規需求總結)

**Phase 1 (MVP) 必須達成:**
- 交易記錄不可篡改(Append-Only Table)
- 完整稽核軌跡(含 trace_id 整合)
- 完整交易歷史查詢功能
- 基本 JWT 認證機制
- 密碼 hash 儲存(bcrypt/Argon2)

**Phase 1 明確標註的技術債務(可接受):**
- 敏感資料明文儲存(非加密)
- 無 API Rate Limiting
- 簡化 JWT 機制(無 refresh token)
- 無詐欺偵測功能
- 無合規報表生成

**Phase 2/3 生產級別增強:**
- KYC/AML 流程整合
- PCI DSS 合規認證
- 敏感欄位加密 + KMS
- 詐欺偵測引擎
- 合規報表自動生成
- GDPR/CCPA 完整合規

---

### Industry Standards & Best Practices (業界標準與最佳實踐)

**Demo 已遵循的業界標準:**
- **SAGA Pattern:** 分散式交易補償邏輯(金融系統核心模式)
- **Event Sourcing 概念:** 交易記錄 append-only,完整事件歷程
- **Idempotency:** 交易操作冪等性設計(防止重複扣款)
- **同步交易處理:** 轉帳/換匯採用同步處理,符合金融業務需求(使用者需立即知道結果)
- **Audit Trail 整合可觀測性:** trace_id 關聯交易記錄,業界領先實踐

**Phase 2/3 可加入的業界標準:**
- **ISO 27001:** 資訊安全管理系統
- **SOC 2 Type II:** 服務組織控制報告
- **Open Banking API Standards:** 開放銀行 API 標準(如 UK Open Banking、PSD2)

---

### Required Expertise & Validation (所需專業知識與驗證)

**Demo 開發所需專業知識:**
- **分散式交易模式:** SAGA Pattern、Two-Phase Commit 概念
- **資料一致性:** Eventual Consistency vs Strong Consistency 權衡
- **可觀測性整合:** OpenTelemetry SDK、log ↔ trace ↔ metric 三本柱
- **金融業務邏輯:** 多幣別管理、換匯流程、補償邏輯

**生產環境額外需求(Phase 2/3):**
- **KYC/AML 法規專業:** 區域法規差異(台灣、美國、歐盟)
- **PCI DSS 認證專業:** 支付卡產業資料安全標準
- **資訊安全專業:** 滲透測試、安全稽核、威脅模型分析

**驗證方式:**
- **功能驗證:** 完整 Demo 流程執行,所有交易功能正常
- **可觀測性驗證:** trace_id 正確關聯交易記錄,三向跳轉功能正常
- **資料完整性驗證:** 交易記錄不可篡改,SAGA 補償邏輯執行正確
- **安全驗證(Phase 2):** 滲透測試、安全掃描、漏洞評估

---

### Implementation Considerations (實作考量)

**架構設計決策(已考慮 Fintech 特性):**

1. **同步 vs 異步處理決策:**
   - **轉帳/換匯:** 同步處理(SAGA Pattern),符合金融業務需求
   - **通知服務:** 異步處理(Kafka),不影響交易主流程
   - **理由:** 金融交易需立即回饋結果給使用者,不可接受「稍後通知交易結果」

2. **交易完整性保證:**
   - **SAGA 補償邏輯:** 失敗時自動執行補償(unfreeze balance → rollback transaction)
   - **Idempotency Key:** 每筆交易唯一 transaction_id,防止重複扣款
   - **Append-Only Record:** 交易記錄不可修改,確保稽核軌跡完整

3. **可觀測性整合(金融領域關鍵):**
   - **trace_id 關聯交易記錄:** 每筆交易可快速跳轉至完整 trace
   - **SAGA 狀態可視化:** Grafana Tempo 清楚顯示 6 個步驟與補償邏輯
   - **業務語義 span attributes:** account.id, transaction.amount, exchange.rate 等

4. **技術債務管理(明確標註):**
   - 敏感資料明文儲存 → Phase 2 加入欄位加密
   - 簡化 JWT 認證 → Phase 2 加入 refresh token 機制
   - 無詐欺偵測 → Phase 3 加入即時詐欺偵測引擎
   - 無合規報表 → Phase 2 加入自動報表生成

**這些技術債務不影響 Demo 核心價值(可觀測性展示),但在真實生產環境部署前必須解決。**

---

## Innovation & Novel Patterns (創新與新穎模式)

### 創新定位說明

**lite-bank-demo** 的創新不在於「發明新技術」,而在於「將現有最佳實踐組合成完整展示流程」,並透過 Workshop 即時演示,讓觀眾親眼見證可觀測性的決定性價值。這是「執行創新」(Execution Innovation)而非「技術創新」(Technical Innovation)。

### Detected Innovation Areas (檢測到的創新領域)

#### 1. Chaos Engineering 與 Observability 的即時整合展示(核心創新)

**創新本質:**
不是 Chaos Mesh 或 OpenTelemetry 本身的創新,而是 **Workshop 現場「即時故障注入 → 即時 trace 定位」的完整展示流程**。

**完整展示流程:**
1. **即時觸發:** 主講者在 Chaos Mesh UI 點擊「注入 NetworkChaos:exchange-service 延遲 5 秒」
2. **立即執行交易:** 前端轉帳頁面點擊「轉帳 USD 100」,畫面顯示處理中...
3. **1 分鐘內定位根因:** 主講者打開 Grafana Tempo,複製 trace_id,立即看到:
   - exchange-service 的 span 顯示 5.2 秒延遲(紅色標記)
   - SAGA 補償邏輯自動執行(unfreeze balance → rollback transaction)
   - 完整 6 個步驟清楚可見

**為什麼這是創新:**
- **即時性:** 不是事後分析,而是觀眾親眼見證「故障發生 → 立即定位」的完整過程
- **真實性:** 不是刻意設計的 demo 錯誤,而是 Chaos Mesh 模擬真實生產故障
- **視覺化衝擊:** 10-15 分鐘手動拼湊 vs 1-2 分鐘自動定位的強烈對比

#### 2. 手動 SDK Instrumentation 跨語言/跨協議完整覆蓋(最佳實踐組合)

**技術組合:**
- **跨語言:** Java SpringBoot → Python FastAPI → Java Quarkus
- **跨協議:** 同步 REST API + 異步 Kafka + WebSocket 推播
- **前端整合:** React OpenTelemetry Browser SDK → 後端完整 trace propagation

**為什麼這是最佳實踐組合:**
- **手動 SDK 提供業務語義:** 自動 instrumentation 無法理解業務邏輯,手動 SDK 確保語義完整
- **完整性:** 從前端到後端,從同步到異步,從 request 到 response,無斷點
- **重建信心:** 證明「正確使用 SDK 才是可觀測性成功的關鍵」

**驗證方式:**
- 完整 trace 在 Grafana Tempo 清楚呈現 7 個微服務的調用鏈路
- 協調層服務（Transfer、Exchange、Deposit-Withdrawal）呼叫資料層服務的完整鏈路可見
- 自定義 span attributes 包含業務語義(account.id, transaction.amount, exchange.rate)

#### 3. SAGA Pattern 自然可視化(利用 Tempo 原生能力)

**正確做法:**
- **不是:** 自己開發專門的 SAGA workflow 可視化工具
- **而是:** 正確使用 OpenTelemetry SDK,讓 Grafana Tempo **自然呈現** SAGA 流程

**SAGA 步驟在 Tempo 的呈現（轉帳範例）:**
1. `validate-source-account` span (Account Service)
2. `validate-destination-account` span (Account Service)
3. `debit-from-account` span (Transaction Service)
4. `credit-to-account` span (Transaction Service)
5. `record-transfer-out` span (Transaction Service)
6. `record-transfer-in` span (Transaction Service)

**補償邏輯可見性:**
- 失敗時,Tempo 顯示補償 spans:`unfreeze-balance` → `rollback-transaction`
- span status 顯示 ERROR,span attributes 包含錯誤訊息

**為什麼這是正確做法:**
- **利用標準工具:** 不重新發明輪子,利用 Grafana Tempo 原生能力
- **生產級別思維:** 真實生產環境也是用 Tempo 看 trace,不會有專門的 workflow UI
- **降低維護成本:** 不需要維護額外的可視化工具

#### 4. AI SRE 根因分析(Phase 2 前瞻探索)

**Phase 2 創新方向:**
- **LLM 自動分析:** 從 Grafana Tempo + Loki + Prometheus 自動取得資料
- **根因報告生成:** LLM 分析 trace、logs、metrics,產生根因分析報告
- **智能維運:** 從「人工分析 trace」提升到「AI 自動分析」

**明確前提條件(Phase 1 必須先完成):**
- **金融系統功能完整:** 所有銀行功能(存提款、轉帳、換匯)正常運作
- **Observability 完整:** log ↔ trace ↔ metric 三向跳轉正常
- **資料品質:** trace、logs、metrics 資料完整且正確

**Phase 2 驗證方式:**
- LLM 產生的根因報告品質評估
- 與人工分析結果對比準確度
- 分析速度提升(秒級 vs 分鐘級)

---

### Market Context & Competitive Landscape (市場背景與競爭格局)

**現有工具與方案:**

**1. Observability 工具:**
- **Grafana Stack (Tempo + Loki + Prometheus):** 業界標準,開源免費
- **Datadog / New Relic:** 商業 APM,功能強大但成本高
- **Elastic APM:** 基於 Elasticsearch,適合已有 ELK Stack 的團隊

**2. Chaos Engineering 工具:**
- **Chaos Mesh:** Kubernetes 原生,CNCF 孵化專案
- **Litmus Chaos:** 另一個 K8s Chaos 工具
- **AWS Fault Injection Simulator:** AWS 雲端服務

**3. Workflow Orchestration 工具:**
- **Camunda / Temporal:** 專門的 workflow 引擎,有完整 UI
- **Apache Airflow:** 資料工程 workflow

**lite-bank-demo 的差異化定位:**

| 面向 | 現有工具 | lite-bank-demo |
|------|---------|----------------|
| **目標** | 生產環境使用 | 技術展示與教學 |
| **整合度** | 需要分別設定 | 完整整合展示流程 |
| **即時性** | 事後分析為主 | Workshop 即時演示 |
| **SAGA 可視化** | 需專門 workflow 引擎 | 利用 Tempo 原生能力 |
| **Chaos 整合** | 獨立測試工具 | 即時展示工具 |
| **學習曲線** | 複雜,需長時間學習 | 15-20 分鐘完整展示 |

**市場定位:**
- **不是:** 與 Datadog / Camunda 競爭的生產工具
- **而是:** 技術啟蒙與教學的完整參考架構
- **目標:** 讓團隊理解「完整可觀測性的可能性」,建立信心後再選擇生產工具

---

### Validation Approach (驗證方法)

#### Phase 1 (MVP) 驗證標準:

**1. 金融系統功能驗證:**
- 所有銀行功能(存提款、轉帳、換匯、交易歷史查詢)正常運作
- SAGA 補償邏輯在失敗時正確執行
- 交易資料一致性驗證(餘額正確、交易記錄完整)

**2. Observability 完整性驗證:**
- **Trace 完整性:** 從 React 前端到 WebSocket 推播,完整 trace_id 傳遞無斷點
- **Log ↔ Trace 跳轉:** 從 Grafana Loki logs 點擊 trace_id 可跳轉至 Tempo
- **Metric ↔ Trace 跳轉:** 從 Prometheus metrics exemplar 可跳轉至 Tempo
- **Span Attributes 完整:** account.id, transaction.amount, exchange.rate 等業務語義資訊清楚可見

**3. Chaos Engineering 即時整合驗證:**
- **Chaos Mesh 故障注入:** NetworkChaos、PodChaos、StressChaos 可成功觸發
- **即時定位:** 故障觸發後 1-2 分鐘內,Grafana Tempo 清楚顯示根因
- **Workshop 流暢性:** 15-20 分鐘完整展示流程,無嚴重 Bug

**4. SAGA 可視化驗證:**
- **6 個步驟清楚可見:** Grafana Tempo 顯示完整 SAGA 流程(freeze → query → deduct → credit → record → notify)
- **補償邏輯可見:** 失敗時,Tempo 顯示補償 spans(unfreeze → rollback)
- **執行時間可見:** 每個 span 的執行時間清楚標示,延遲問題一目了然

#### Phase 2 (AI SRE) 驗證標準:

**前提條件驗證:**
- Phase 1 所有功能 100% 正常運作
- Observability 資料品質穩定且完整

**AI SRE 驗證:**
- **資料取得:** LLM 能正確從 Tempo + Loki + Prometheus 取得資料
- **根因報告品質:** 與人工分析結果對比,準確度 ≥ 80%
- **分析速度:** 秒級產生報告(vs 人工分鐘級)

---

### Risk Mitigation (風險緩解)

#### Phase 1 風險與緩解策略:

**風險 1: Workshop 現場 Demo 失敗**
- **風險描述:** 網路問題、K8s 環境不穩定、Chaos 注入導致系統崩潰
- **緩解策略:**
  - 預先錄製備用影片(完整 Demo 流程)
  - 本地 K8s 環境(minikube/kind)+ 雲端 K8s 環境雙備援
  - Chaos 場景預先測試,確保可控且可恢復

**風險 2: SAGA 可視化不如預期清楚**
- **風險描述:** Grafana Tempo 呈現過於複雜,觀眾看不懂
- **緩解策略:**
  - 優化 span 命名(清楚描述每個步驟)
  - 使用 span attributes 標註業務語義(account.id, amount, currency)
  - 預先準備 Tempo 查詢範例(如何快速定位 SAGA 流程)

**風險 3: 跨語言 trace propagation 斷鏈**
- **風險描述:** Kafka 或 Quarkus 的 trace_id 傳遞失敗
- **緩解策略:**
  - 完整測試覆蓋(unit test + integration test)
  - 預先驗證 Kafka headers 正確傳遞 W3C TraceContext
  - Quarkus OpenTelemetry SDK 手動設定(確保不依賴 auto-instrumentation)

**風險 4: 觀眾技術背景差異大**
- **風險描述:** 初階 SRE 看不懂 Tempo,資深 Lead 覺得太簡單
- **緩解策略:**
  - 分層展示:先展示視覺化衝擊(10-15 分鐘 vs 1-2 分鐘),再深入技術細節
  - 準備 Q&A 環節,針對不同技術層級提供額外說明
  - 提供完整 GitHub repo,讓觀眾可自行深入研究

#### Phase 2 風險與緩解策略:

**風險 5: AI SRE 報告品質不穩定**
- **風險描述:** LLM 產生的根因分析不準確或幻覺(hallucination)
- **緩解策略:**
  - 使用結構化 prompt,限制 LLM 輸出格式
  - 人工驗證機制,比對 LLM 分析與實際根因
  - 僅作為「輔助工具」,不取代人工最終判斷

**風險 6: Phase 1 未完成就貿然進入 Phase 2**
- **風險描述:** 基礎不穩,AI SRE 無法正常運作
- **緩解策略:**
  - 明確 Phase 1 完成標準(金融系統 + Observability 100% 正常)
  - Phase 1 完整驗證通過後,才啟動 Phase 2
  - Phase 2 為「加分項」,Phase 1 成功就是專案成功

---

## Web App + Developer Tool Specific Requirements

### Project-Type Overview

**lite-bank-demo** 是一個 **混合型技術展示專案**,結合了 Web App 的完整使用者介面與 Developer Tool 的教學屬性。專案定位為「可執行的技術教材」,既需要流暢的前端體驗來展示可觀測性效果,也需要完整的部署文件與範例讓觀眾能自行複製學習。

**專案類型組合:**
- **60% Developer Tool 屬性:** 技術展示、教學工具、Workshop 素材
- **40% Web App 屬性:** 完整前端 UI、使用者互動、即時更新

**核心定位:** 技術啟蒙工具,而非生產級應用程式。

---

### Technical Architecture Considerations

#### 1. 前端架構決策 (Web App)

**SPA (Single Page Application) 架構:**
- **技術選型:** React 18+ with TypeScript
- **路由管理:** React Router v6 (客戶端路由)
- **狀態管理:** React Context API (簡化版,避免過度工程)
- **UI 框架:** Material-UI 或 Ant Design (生產級別質感)

**瀏覽器支援範圍 (現代瀏覽器優先):**
- **主要支援:** Chrome 100+, Edge 100+, Firefox 100+
- **次要支援:** Safari 15+ (Mac 環境常見)
- **不支援:** IE 11, 舊版瀏覽器 (Demo 不需要相容性負擔)

**前端效能目標 (Workshop 展示需求):**
- **首次載入時間:** ≤ 3 秒 (可接受範圍)
- **頁面切換響應:** ≤ 500ms (SPA 優勢,流暢切換)
- **交易提交響應:** ≤ 2 秒 (包含後端 SAGA 處理時間)
- **技術債務:** 無效能優化 (Code Splitting, Lazy Loading),Phase 1 不實作

**SEO 與 Accessibility (Demo 不要求):**
- **不實作 SEO:** Workshop 內部展示,不需要搜尋引擎索引
- **不實作完整 Accessibility:** 無 WCAG 2.1 AA 合規要求
- **基本可用性:** 確保鍵盤導航、基本語義化 HTML

---

#### 2. 即時更新機制 (Real-time Features)

**WebSocket 即時通知整合 (核心展示功能):**

**實作方式 (基於協調層架構):**
1. **交易流程:**
   - 使用者提交轉帳/換匯 → 協調層服務 (Transfer/Exchange Service) 處理
   - 協調層呼叫 Account Service 驗證 → Transaction Service 執行金流操作
   - 操作完成後返回結果給前端

2. **前端處理邏輯:**
   - React 前端收到操作結果後,**自動呼叫 Account Service API** 重新查詢帳戶餘額
   - 更新前端 UI 顯示最新餘額與交易狀態

3. **Trace Propagation 整合 (關鍵技術展示):**
   - 所有服務間呼叫包含 `traceparent` header
   - 前端發起請求時,在 OpenTelemetry Browser SDK 中生成 trace
   - 完整 trace 鏈路:React → API Gateway → 協調層 → 資料層

**即時更新場景清單:**
- **交易完成通知:** 轉帳/換匯完成後,WebSocket 推送通知 + 自動刷新餘額
- **SAGA 補償通知:** 交易失敗時,推送補償邏輯執行狀態
- **不實作持續推送:** 不主動推送餘額變化(僅在交易完成時觸發)
- **不實作多使用者即時同步:** Demo 限單一使用者情境

**複雜度評估與權衡:**
- **保留 WebSocket:** 已是架構核心,展示 Kafka → WebSocket trace propagation
- **簡化實作:** 僅在交易完成時觸發更新,不需要複雜的狀態同步
- **技術債務:** WebSocket 連線管理簡化(無斷線重連、心跳機制),Phase 1 可接受

---

#### 3. OpenTelemetry 前端整合 (Trace Propagation)

**React OpenTelemetry Browser SDK 實作:**

**Instrumentation 範圍:**
- **自動追蹤:** XMLHttpRequest, Fetch API (所有 API 呼叫)
- **自定義 Span:** 關鍵使用者操作(轉帳提交、換匯請求)
- **WebSocket Trace Context:** WebSocket 連線建立與訊息接收
- **頁面載入追蹤:** Document Load, Resource Timing

**Span Attributes 設計 (業務語義):**
- **使用者資訊:** `user.id`, `account.id`
- **交易資訊:** `transaction.type`, `transaction.amount`, `transaction.currency`
- **前端資訊:** `browser.name`, `browser.version`, `page.url`

**Trace Context Propagation (W3C TraceContext):**
- **HTTP Headers:** 所有 API 請求包含 `traceparent`, `tracestate` headers
- **WebSocket:** 連線建立時在 query string 或首次訊息中傳遞 trace context
- **完整鏈路:** React Browser SDK → API Gateway → 後端微服務 → Kafka → WebSocket

**驗證標準:**
- 在 Grafana Tempo 看到完整 trace,從 React 前端到 WebSocket 推播無斷點
- 前端 span 包含正確的業務語義 attributes
- trace_id 在前端 Console、後端 Logs、Grafana Tempo 三處一致

---

### Developer Tool Requirements

#### 1. 部署自動化 (Deployment Automation)

**提供完整部署方案 (多環境支援):**

**方案 1: Helm Charts (K8s 生產級別部署):**
- **提供範圍:** 完整 Helm Charts 包含所有微服務、Grafana Stack、Kafka、PostgreSQL
- **部署指令:**
  ```bash
  helm install lite-bank-demo ./helm/lite-bank-demo -n demo --create-namespace
  ```
- **環境變數管理:** values.yaml 分離(dev, staging, prod)
- **健康檢查:** Kubernetes Liveness/Readiness Probes

**方案 2: Docker Compose (本地快速啟動):**
- **提供範圍:** docker-compose.yml 包含所有服務(簡化版,無 K8s 依賴)
- **啟動指令:**
  ```bash
  docker-compose up -d
  ```
- **適用場景:** 觀眾想在本地筆電快速驗證,無需 K8s 環境
- **限制:** Chaos Mesh 無法在 Docker Compose 執行(需要 K8s)

**方案 3: Kustomize (K8s 進階客製化):**
- **提供範圍:** base + overlays (dev/prod 環境差異)
- **部署指令:**
  ```bash
  kubectl apply -k overlays/dev
  ```
- **適用場景:** 資深 K8s 使用者,需要更靈活的客製化

**部署文件要求:**
- **README.md:** 快速開始指南(5 分鐘內啟動系統)
- **DEPLOYMENT.md:** 詳細部署步驟、環境需求、故障排查
- **ARCHITECTURE.md:** 系統架構圖、服務依賴關係、Trace 鏈路說明

---

#### 2. 教學文件與範例 (Documentation & Examples)

**提供完整教學資源 (自學友善):**

**文件結構:**
```
docs/
├── 01-quickstart.md          # 5 分鐘快速開始
├── 02-architecture.md         # 系統架構詳解
├── 03-observability.md        # 可觀測性整合說明
├── 04-saga-pattern.md         # SAGA 模式實作細節
├── 05-chaos-engineering.md    # Chaos Mesh 使用指南
├── 06-troubleshooting.md      # 常見問題排查
└── 07-workshop-script.md      # Workshop 演講腳本(15-20 分鐘)
```

**程式碼範例 (Code Examples):**
- **OpenTelemetry SDK 手動 instrumentation 範例:**
  - Java SpringBoot 範例(Account Service)
  - Java Quarkus 範例(Exchange Service)
  - Python FastAPI 範例(如有使用)
  - React Browser SDK 範例
- **Kafka Trace Propagation 範例:** Producer 與 Consumer 如何傳遞 trace context
- **SAGA 補償邏輯範例:** 失敗時如何執行補償 transaction

**Workshop 腳本 (演講者使用):**
- **15-20 分鐘完整流程:**
  1. 問題場景介紹(10-15 分鐘手動排查痛點)
  2. 系統架構快速導覽(2 分鐘)
  3. Chaos Mesh 故障注入展示(2 分鐘)
  4. Grafana Tempo 即時定位根因(2 分鐘)
  5. SAGA 可視化展示(2 分鐘)
  6. log ↔ trace ↔ metric 三向跳轉(2 分鐘)
  7. Q&A 與總結(5 分鐘)
- **逐步操作指令:** 每個步驟的確切指令、預期結果、截圖參考

---

#### 3. 客製化與擴展性 (Customization)

**明確定位: 提供完整 Code,但不設計客製化介面**

**開發者可自行修改的部分 (Source Code Level):**
- **業務邏輯:** 帳戶類型、幣別、交易規則(需修改程式碼)
- **Chaos 場景:** 新增自定義 Chaos Mesh YAML (需了解 Chaos Mesh)
- **SAGA 流程:** 修改 SAGA 步驟、補償邏輯(需了解 SAGA Pattern)
- **Span Attributes:** 新增自定義 business context(需了解 OpenTelemetry SDK)

**不提供的客製化功能 (Out of Scope):**
- **圖形化設定介面:** 不提供 UI 讓觀眾「點擊設定」Chaos 場景
- **低程式碼平台:** 不提供「拖拉式」workflow 設計工具
- **即時程式碼產生器:** 不提供「根據需求自動產生 SAGA code」

**設計哲學:**
觀眾如果想客製化,應具備以下能力:
- 理解 Java/Spring Boot 基礎
- 理解 OpenTelemetry SDK API
- 理解 Kubernetes YAML
- 理解 Chaos Mesh CRD

**這符合「Developer Tool」定位:給開發者完整範例,而非給非技術人員的黑盒工具。**

---

#### 4. 監控與除錯工具 (Observability for Demo Itself)

**明確定位: 不提供專門的除錯工具**

**依賴標準 K8s/Grafana 工具:**
- **健康檢查:** Kubernetes Liveness/Readiness Probes (標準做法)
- **日誌查詢:** 直接用 Grafana Loki 查詢服務日誌
- **Metrics 監控:** 直接用 Grafana + Prometheus Dashboard
- **Trace 查詢:** 直接用 Grafana Tempo

**不額外開發:**
- **專門的 Debug Mode UI:** 不開發「一鍵切換 debug mode」的管理介面
- **系統健康 Dashboard:** 不開發專門的「服務健康總覽」頁面(直接用 K8s Dashboard)
- **自動故障排查工具:** 不開發「智能診斷」工具(Phase 2 AI SRE 才考慮)

**設計哲學:**
Demo 本身就是展示可觀測性工具,如果需要除錯,**直接使用展示的工具 (Tempo/Loki/Prometheus)** 即是最佳示範。

---

### API Design & Integration

#### 1. RESTful API 設計 (前後端整合)

**API 架構:**
- **API Gateway:** 統一入口,包含 JWT 驗證、OpenTelemetry trace propagation
- **服務間通訊:** 同步 REST API (SpringBoot to Quarkus) + 異步 Kafka
- **無版本控制路徑:** API 路徑不包含 `/v1` 等版本號(依您的需求)

**核心 API Endpoints (前端需要):**

**認證相關:**
- `POST /auth/login` - 使用者登入(返回 JWT token)
- `POST /auth/logout` - 登出(可選實作)

**帳戶相關（Account Service - 資料層，只讀）:**
- `GET /accounts` - 查詢使用者所有帳戶
- `GET /accounts/{accountId}` - 查詢單一帳戶詳情(含餘額)
- `POST /accounts/{accountId}/validate` - 驗證帳戶（狀態、幣別、餘額）

**存提款相關（Deposit-Withdrawal Service - 協調層）:**
- `POST /deposits` - 存款（協調層 → 呼叫 Transaction Service）
- `POST /withdrawals` - 提款（協調層 → 呼叫 Transaction Service）

**轉帳相關（Transfer Service - 協調層）:**
- `POST /transfers` - 轉帳(同幣別)

**換匯相關（Exchange Service - 協調層）:**
- `POST /exchanges` - 換匯轉帳(跨幣別)

**交易記錄（Transaction Service - 資料層，唯一可修改餘額）:**
- `POST /transactions` - 建立交易記錄（同時更新餘額）
- `GET /transactions?accountId={id}` - 交易歷史查詢

**匯率相關（Exchange Rate Service - 資料層）:**
- `GET /rates/{from}/{to}` - 查詢即時匯率(模擬)

**OpenTelemetry 整合:**
- **所有 API 回應包含 `X-Trace-Id` header** (方便前端記錄與除錯)
- **錯誤回應包含 trace_id** (JSON 中的 `trace_id` 欄位)

---

#### 2. WebSocket API 設計

**WebSocket 連線 Endpoint:**
- `ws://api-gateway/ws/notifications?token={jwt_token}`

**訊息格式 (JSON):**
```json
{
  "type": "TRANSACTION_COMPLETED",
  "trace_id": "abc123def456...",
  "transaction_id": "txn-uuid-123",
  "account_id": "acc-uuid-456",
  "message": "Transfer completed successfully",
  "timestamp": "2025-12-11T10:30:00Z"
}
```

**前端處理流程:**
1. 登入後建立 WebSocket 連線(傳遞 JWT token 認證)
2. 監聽訊息事件
3. 收到 `TRANSACTION_COMPLETED` 訊息後:
   - 顯示通知給使用者(Toast/Snackbar)
   - 自動呼叫 `GET /accounts/{accountId}` 重新查詢餘額
   - 在 OpenTelemetry Browser SDK 記錄 span(關聯 trace_id)

---

### Performance & Scalability

#### 1. 效能目標 (Demo 等級)

**前端效能:**
- **首次載入:** ≤ 3 秒 (可接受)
- **API 回應:** ≤ 2 秒 (包含 SAGA 處理)
- **頁面切換:** ≤ 500ms (SPA 流暢切換)

**後端效能:**
- **SAGA 交易處理:** ≤ 2 秒 (正常情境)
- **Chaos 注入情境:** 可接受 5-10 秒 (展示補償邏輯)
- **無效能測試:** Phase 1 不進行 Load Testing、Stress Testing

**可擴展性 (Demo 不要求):**
- **不實作水平擴展:** 每個服務僅 1-2 個 Pod
- **不實作 Auto Scaling:** 無 HPA (Horizontal Pod Autoscaler)
- **不實作 Load Balancing 優化:** 依賴 K8s Service 預設行為

---

#### 2. 資料庫效能 (Demo 等級)

**PostgreSQL 設定:**
- **單一 PostgreSQL 實例** (不需要 HA/Replication)
- **基本索引:** 交易查詢相關欄位(`account_id`, `created_at`, `trace_id`)
- **無查詢優化:** 無 Query Plan 分析、無 Connection Pool 調校

**資料量限制 (Demo 可接受):**
- **測試資料:** 10-20 個帳戶、100-200 筆交易記錄
- **無大量資料測試:** 不驗證萬筆/百萬筆交易情境

---

### Security Considerations (Demo Level)

**已在 Domain-Specific Requirements (Fintech) 詳細定義,此處僅列摘要:**

**Phase 1 必須實作:**
- JWT 認證(簡化版)
- 密碼 hash 儲存(bcrypt/Argon2)
- HTTPS/TLS (K8s Ingress)

**Phase 1 明確技術債務 (可接受):**
- 敏感資料明文儲存
- 無 API Rate Limiting
- 簡化 JWT 機制

---

### Implementation Considerations

#### 1. 技術棧總結

**前端:**
- React 18+ with TypeScript
- React Router v6
- Material-UI / Ant Design
- OpenTelemetry Browser SDK
- WebSocket Client (native WebSocket API)

**後端:**
- Java SpringBoot 3.x (Account Service, Payment Service, Transaction Service)
- Java Quarkus 3.x (Exchange Service,展示 Quarkus 手動 SDK instrumentation)
- Python FastAPI (可選,若有需要)
- Kafka KRaft mode (異步通訊)
- PostgreSQL (資料庫)

**可觀測性:**
- Grafana Tempo (Distributed Tracing)
- Grafana Loki (Log Aggregation)
- Prometheus (Metrics)
- Grafana (Dashboard)

**混沌工程:**
- Chaos Mesh (Kubernetes Chaos Engineering)

**部署:**
- Kubernetes (minikube/kind 本地 或 GKE/EKS 雲端)
- Helm Charts
- Docker Compose (本地快速啟動)

---

#### 2. 開發優先順序 (Phase 1 MVP)

**P0 (必須完成):**
1. 所有銀行功能正常(存提款、轉帳、換匯)
2. SAGA 補償邏輯完整
3. OpenTelemetry 完整 trace propagation (前端到 WebSocket)
4. Grafana Tempo 可視化 SAGA 流程
5. Chaos Mesh 故障注入功能
6. log ↔ trace ↔ metric 三向跳轉

**P1 (重要,但可延後):**
7. 完整部署文件 (Helm + Docker Compose + README)
8. Workshop 腳本
9. UI 美觀優化

**P2 (Nice to Have,可 Phase 2):**
10. 效能優化 (Code Splitting, Lazy Loading)
11. 完整 Error Handling UI
12. AI SRE 根因分析

---

#### 3. 技術風險評估

**高風險 (需優先驗證):**
- **Quarkus OpenTelemetry 手動 SDK:** 確保 trace propagation 正確
- **Kafka trace context 傳遞:** 驗證 Kafka headers 正確傳遞 W3C TraceContext
- **WebSocket trace propagation:** 驗證 WebSocket 訊息包含 trace_id

**中風險 (需測試驗證):**
- **Chaos Mesh 穩定性:** 確保故障注入可控且可恢復
- **SAGA 可視化清晰度:** 確保 Grafana Tempo 呈現易懂

**低風險 (標準實作):**
- React SPA 開發
- JWT 認證
- PostgreSQL 資料庫操作

---

### Success Metrics (Project-Type Specific)

**Web App 成功指標:**
- 前端載入時間 ≤ 3 秒
- 所有頁面切換流暢(≤ 500ms)
- WebSocket 即時通知正常運作
- UI 美觀,展示生產級別質感

**Developer Tool 成功指標:**
- 觀眾可在 15 分鐘內完成本地部署(Helm 或 Docker Compose)
- 文件完整,觀眾可自行學習與客製化
- Workshop 腳本清晰,演講者可順利執行 15-20 分鐘完整展示
- GitHub repo 提供完整 code 與範例

**整合成功指標 (最關鍵):**
- **完整 trace propagation:** 從 React 前端到 WebSocket 推播,無斷點
- **SAGA 可視化:** Grafana Tempo 清楚顯示 6 個步驟與補償邏輯
- **Chaos 即時定位:** 故障注入後 1-2 分鐘內,Tempo 清楚顯示根因
- **三向跳轉:** log ↔ trace ↔ metric 跳轉功能正常

---

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**選定策略:** Experience MVP (體驗優先 MVP)

**核心理念:**
lite-bank-demo 的 MVP 聚焦於「完整展示可觀測性決定性價值」,讓觀眾在 15-20 分鐘 Workshop 內親眼見證「10-15 分鐘手動排查 → 1-2 分鐘自動定位」的效率躍升。為達成這個體驗目標,**Phase 1 專注於後端系統完整性與可觀測性整合**,前端 UI 則在 Phase 1.5 補齊,確保技術核心先完整驗證。

**資源需求 (Phase 1):**
- **1 位資深後端工程師** (熟悉 SAGA、OpenTelemetry、Kafka) - 核心開發者
- **1 位 SRE/DevOps** (Kubernetes、Grafana Stack、Chaos Mesh) - 可觀測性整合與部署
- **預估時程:** 6-8 週 (Phase 1 後端 MVP + Phase 1.5 前端 UI)

**成功標準:**
- Workshop 演講者可透過 Postman 執行完整 Demo 流程(Phase 1)
- Grafana Tempo 清楚顯示 SAGA 6 個步驟與補償邏輯
- Chaos Mesh 故障注入後 1-2 分鐘內定位根因
- log ↔ trace ↔ metric 三向跳轉功能完整

---

### MVP Feature Set (Phase 1: Backend + Observability Core)

**Phase 1 開發範圍 (6 週):**

#### 1. 銀行核心功能 (完整實作)

**所有 4 項銀行功能必須完整（協調層模式）:**
- ✅ **存款 (Deposit):** POST `/deposits`（Deposit-Withdrawal Service → Transaction Service）
- ✅ **提款 (Withdraw):** POST `/withdrawals`（Deposit-Withdrawal Service → Transaction Service）
- ✅ **轉帳 (Transfer):** POST `/transfers`（Transfer Service → Account Service + Transaction Service）
- ✅ **換匯 (Exchange):** POST `/exchanges`（Exchange Service → Account Service + Exchange Rate Service + Transaction Service）

**SAGA 流程完整性（協調層架構）:**
- 協調層服務（Transfer、Exchange）負責業務流程編排
- 所有金流操作統一透過 Transaction Service 執行
- 失敗時補償邏輯由協調層處理：反向呼叫 Transaction Service
- 所有步驟在 Grafana Tempo 清楚可見

#### 2. 交易歷史查詢 (必須完成)

**功能需求:**
- ✅ **API Endpoint:** GET `/transactions/history?accountId={id}&startDate={date}&endDate={date}`
- ✅ **回應資料包含:**
  - `transaction_id`, `trace_id` (關鍵!可跳轉至 Grafana Tempo)
  - `account_id_from`, `account_id_to`
  - `amount`, `currency`, `transaction_type`
  - `status` (PENDING, COMPLETED, FAILED, COMPENSATED)
  - `created_at`, `metadata` (SAGA 補償記錄)

**可觀測性整合:**
- 每筆交易記錄包含對應的 `trace_id`
- Postman 測試時,可複製 `trace_id` 並在 Grafana Tempo 查詢完整 trace
- 展示「從交易記錄跳轉至完整 trace 鏈路」的能力

#### 3. 完整可觀測性整合 (Phase 1 核心)

**OpenTelemetry 手動 SDK Instrumentation:**
- ✅ Java SpringBoot (Account Service, Payment Service, Transaction Service)
- ✅ Java Quarkus (Exchange Service,展示 Quarkus 手動 SDK)
- ✅ Kafka trace propagation (Producer → Consumer trace context 傳遞)
- ✅ 自定義 span attributes (account.id, transaction.amount, exchange.rate)

**Grafana Stack 完整整合:**
- ✅ Grafana Tempo:完整 trace 可視化,SAGA 流程清楚呈現
- ✅ Grafana Loki:log aggregation,與 trace 雙向跳轉
- ✅ Prometheus:metrics 收集,與 trace 雙向跳轉
- ✅ Grafana Dashboard:統一查詢介面

**Chaos Engineering 整合:**
- ✅ Chaos Mesh 部署與設定
- ✅ 預設 Chaos 場景:NetworkChaos (exchange-service 延遲 5 秒)
- ✅ 故障注入後,Tempo 立即顯示根因

#### 4. 測試與驗證方式 (Phase 1 僅 Postman)

**Postman Collection 提供:**
- ✅ 完整 API 測試集合 (存款、提款、轉帳、換匯、交易查詢)
- ✅ 預設測試資料 (測試帳戶、JWT token)
- ✅ Workshop Demo 腳本對應的 API 呼叫順序
- ✅ Chaos 注入後的失敗情境測試

**前端 UI:**
- ❌ **Phase 1 不實作前端 UI** (延後至 Phase 1.5)

**理由:**
優先確保後端 SAGA、OpenTelemetry、Chaos 整合完全正確,避免前端開發分散資源。Postman 足以完成 Workshop Demo 核心展示。

#### 5. 部署自動化 (Phase 1 基本部署)

**提供部署方案:**
- ✅ **Helm Charts:** 完整 K8s 部署 (所有微服務 + Grafana Stack + Kafka + PostgreSQL)
- ✅ **Docker Compose:** 本地快速啟動 (簡化版,無 Chaos Mesh)
- ✅ **基本 README.md:** 快速開始指南 (5 分鐘內啟動系統)
- ✅ **基本 DEPLOYMENT.md:** 部署步驟、環境需求、基本故障排查

**文件範圍 (Phase 1 最小化):**
- ✅ README.md:快速開始 + 基本架構說明
- ✅ DEPLOYMENT.md:Helm/Docker Compose 部署步驟
- ❌ 詳細技術文件 (延後至 Phase 1.5)

---

### Post-MVP Features (Phased Development Roadmap)

#### Phase 1.5: Frontend UI (2-3 週)

**新增功能:**
- ✅ **React 前端 UI (SPA):**
  - 登入頁面 (JWT 認證)
  - 帳戶餘額頁面 (顯示多幣別餘額)
  - 轉帳/換匯頁面 (表單提交)
  - 交易歷史頁面 (列表顯示 + trace_id 可點擊跳轉)

- ✅ **WebSocket 即時通知:**
  - 交易完成後 WebSocket 推送通知
  - 前端自動刷新帳戶餘額
  - WebSocket trace propagation 整合

- ✅ **OpenTelemetry Browser SDK:**
  - 前端到後端完整 trace propagation
  - 使用者操作追蹤 (轉帳提交、換匯請求)
  - 完整鏈路:React → API Gateway → 後端微服務 → Kafka → WebSocket

**成功標準:**
- Workshop 演講者可用 UI 執行完整 Demo (不再依賴 Postman)
- 前端 trace 在 Grafana Tempo 與後端 trace 正確串接
- WebSocket 通知觸發後,餘額自動更新

---

#### Phase 2: Documentation & Workshop Materials (2 週)

**新增文件:**
- ✅ **完整技術文件 (7 份核心文件):**
  1. `01-quickstart.md` - 5 分鐘快速開始
  2. `02-architecture.md` - 系統架構詳解
  3. `03-observability.md` - 可觀測性整合說明
  4. `04-saga-pattern.md` - SAGA 模式實作細節
  5. `05-chaos-engineering.md` - Chaos Mesh 使用指南
  6. `06-troubleshooting.md` - 常見問題排查
  7. `07-workshop-script.md` - Workshop 演講腳本 (15-20 分鐘)

- ✅ **程式碼範例 (Code Examples):**
  - OpenTelemetry SDK 手動 instrumentation 範例 (SpringBoot/Quarkus/React)
  - Kafka trace propagation 範例
  - SAGA 補償邏輯範例

- ✅ **Workshop 演講腳本:**
  - 15-20 分鐘逐步操作指令
  - 預期結果與截圖參考
  - Q&A 常見問題與回答

**成功標準:**
- 觀眾可依文件自行完成本地部署 (15 分鐘內)
- 演講者可依 Workshop 腳本順利執行完整展示
- GitHub repo 完整,可作為技術教材

---

#### Phase 3: Advanced Features (未來探索)

**AI SRE 根因自動分析 (前瞻性探索):**
- ⚠️ **前提條件:** Phase 1 + 1.5 + 2 全部完成且穩定運作
- 🔬 **探索目標:**
  - LLM 自動從 Tempo + Loki + Prometheus 取得資料
  - 產生根因分析報告 (vs 人工分析對比)
  - 分析速度提升 (秒級 vs 分鐘級)

**金融合規增強 (生產級別考量):**
- KYC/AML 流程整合架構
- PCI DSS 合規認證準備
- 敏感欄位加密 + KMS 整合

**效能優化與測試:**
- Load Testing (k6 或 JMeter)
- 前端效能優化 (Code Splitting, Lazy Loading)
- 資料庫查詢優化

**明確聲明:**
Phase 3 為「未來考慮」,**不影響 Phase 1 專案成功**。Phase 1 成功標準是「技術啟蒙效果」,而非「生產級別完整性」。

---

### Risk Mitigation Strategy (風險緩解策略)

#### Technical Risks (技術風險)

**風險 1: Quarkus OpenTelemetry 手動 SDK 整合失敗**
- **風險等級:** 高
- **影響範圍:** 完整 trace propagation 中斷
- **緩解策略:**
  - Week 2 優先驗證 Quarkus → SpringBoot trace 傳遞
  - 預留備案:若 Quarkus 整合失敗,改用 SpringBoot 實作 Exchange Service
  - 參考官方文件:Quarkus OpenTelemetry Extension 手動設定指南

**風險 2: Kafka trace context 傳遞斷鏈**
- **風險等級:** 高
- **影響範圍:** 異步通訊追蹤失效
- **緩解策略:**
  - Week 3 優先驗證 Kafka Producer/Consumer trace propagation
  - 確保 Kafka headers 正確傳遞 W3C TraceContext (`traceparent`, `tracestate`)
  - 完整 integration test 覆蓋

**風險 3: SAGA 可視化在 Tempo 不夠清楚**
- **風險等級:** 中
- **影響範圍:** Workshop 展示效果不佳
- **緩解策略:**
  - 優化 span 命名 (清楚描述每個步驟:`freeze-balance`, `query-exchange-rate`)
  - 使用 span attributes 標註業務語義 (`account.id`, `amount`, `currency`)
  - 預先準備 Tempo 查詢範例 (如何快速定位 SAGA 流程)

---

#### Market Risks (市場風險)

**風險 4: 觀眾技術背景差異大,展示無法滿足所有人**
- **風險等級:** 中
- **影響範圍:** 技術啟蒙效果打折
- **緩解策略:**
  - **分層展示:** 先展示視覺化衝擊 (10-15 分鐘 vs 1-2 分鐘),再深入技術細節
  - **Q&A 環節:** 針對不同技術層級提供額外說明
  - **完整 GitHub repo:** 讓觀眾可自行深入研究

**風險 5: Workshop 現場 Demo 失敗 (網路、K8s 環境問題)**
- **風險等級:** 高
- **影響範圍:** 技術信任度受損
- **緩解策略:**
  - **預先錄製備用影片** (完整 Demo 流程,15-20 分鐘)
  - **雙環境備援:** 本地 K8s (minikube/kind) + 雲端 K8s (GKE/EKS)
  - **Chaos 場景預先測試:** 確保故障可控且可恢復

---

#### Resource Risks (資源風險)

**風險 6: 資深後端工程師資源不足 (人力短缺)**
- **風險等級:** 高
- **影響範圍:** 開發時程延遲
- **緩解策略:**
  - **最小化 Phase 1 範圍:** 僅後端 + Postman 測試,前端延後至 Phase 1.5
  - **技術債務可接受:** 明確標註簡化項目 (無 refresh token、敏感資料明文)
  - **聚焦核心價值:** SAGA + OpenTelemetry + Chaos 是 P0,其他可延後

**風險 7: Phase 1 時程壓力導致品質妥協**
- **風險等級:** 中
- **影響範圍:** Workshop Demo 不穩定
- **緩解策略:**
  - **嚴格 Phase 1 完成標準:** 所有 4 項銀行功能 + 交易查詢必須 100% 正常
  - **不貿然進入 Phase 1.5:** 後端未穩定前,不開始前端開發
  - **Milestone 檢查點:** Week 4 檢查 SAGA + OpenTelemetry,Week 6 檢查 Chaos 整合

---

### Development Milestones (Phase 1 開發里程碑)

**Week 1-2: 後端微服務基礎架構（資料層）**
- User Service, Account Service, Transaction Service, Exchange Rate Service (SpringBoot)
- PostgreSQL schema 設計 (Append-Only Transaction Table)
- 基本 API endpoints（帳戶查詢、交易記錄）
- 基本 JWT 認證

**Week 2-3: 協調層服務 + OpenTelemetry 整合**
- Deposit-Withdrawal Service, Transfer Service, Exchange Service (SpringBoot)
- 協調層呼叫資料層的完整流程
- 補償邏輯（由協調層處理）
- OpenTelemetry 手動 SDK instrumentation (所有服務)

**Week 3-4: 完整 Trace Propagation 驗證**
- 協調層 → 資料層完整 trace 鏈路驗證
- 所有服務間 traceparent header 傳遞
- 業務語義 span attributes 標註驗證

**Week 4-5: Grafana Stack + Chaos Mesh 整合**
- Grafana Tempo, Loki, Prometheus 部署
- log ↔ trace ↔ metric 三向跳轉驗證
- Chaos Mesh 部署與設定
- 預設 Chaos 場景:NetworkChaos (exchange-service 延遲 5 秒)

**Week 5-6: 交易歷史查詢 + 部署自動化**
- Transaction History API 完成 (含 trace_id 回傳)
- Postman Collection 完整測試集合
- Helm Charts 完整部署方案
- Docker Compose 本地快速啟動
- 基本 README.md + DEPLOYMENT.md

**Week 6: Phase 1 完整驗證與 Workshop 預演**
- 所有 4 項銀行功能完整測試 (Postman)
- SAGA 補償邏輯驗證 (故意觸發失敗情境)
- Chaos 注入後 1-2 分鐘定位根因驗證
- Workshop 15-20 分鐘完整流程預演 (Postman Demo)

---

### Phase 1 Technical Debt (明確可接受的技術債務)

**以下項目 Phase 1 不實作,明確標註為技術債務:**

**安全性簡化:**
- ❌ 敏感資料明文儲存 (帳戶餘額、交易金額) → Phase 2 加入欄位加密
- ❌ 簡化 JWT 認證 (無 refresh token 機制) → Phase 2 完整認證
- ❌ 無 API Rate Limiting → Phase 2 防 DDoS

**功能簡化:**
- ❌ 無詐欺偵測功能 → Phase 3 探索
- ❌ 無合規報表生成 → Phase 2 加入
- ❌ 模擬匯率查詢 (非真實 API) → Phase 2 整合真實匯率 API

**效能簡化:**
- ❌ 無 Load Testing / Stress Testing → Phase 2 效能驗證
- ❌ 無水平擴展 (每服務僅 1-2 Pod) → Phase 2 擴展性優化
- ❌ 無 Auto Scaling (HPA) → Phase 2 加入

**文件簡化:**
- ❌ Phase 1 僅基本 README + DEPLOYMENT → Phase 2 完整 7 份文件
- ❌ 無 Workshop 演講腳本 → Phase 2 加入

**這些技術債務不影響 Phase 1 核心目標 (後端 SAGA + OpenTelemetry + Chaos 完整展示),但在真實生產環境部署前必須解決。**

---

### Success Metrics (Phase 1 成功指標)

**金融系統功能完整性:**
- ✅ 所有 4 項銀行功能 (存提款、轉帳、換匯) 100% 正常運作
- ✅ 交易歷史查詢功能完整 (含 trace_id 回傳)
- ✅ SAGA 補償邏輯在失敗情境正確執行

**可觀測性整合完整性:**
- ✅ 從後端微服務到 Kafka 完整 trace propagation (無斷點)
- ✅ Grafana Tempo 清楚顯示 SAGA 6 個步驟
- ✅ log ↔ trace ↔ metric 三向跳轉功能正常
- ✅ 自定義 span attributes 包含業務語義 (account.id, amount, currency)

**Chaos Engineering 即時整合:**
- ✅ Chaos Mesh NetworkChaos 成功注入故障
- ✅ 故障觸發後 1-2 分鐘內,Grafana Tempo 清楚顯示根因
- ✅ Workshop 預演流暢,無嚴重 Bug

**部署與測試完整性:**
- ✅ Helm Charts 一鍵部署成功 (K8s 環境)
- ✅ Docker Compose 本地快速啟動成功
- ✅ Postman Collection 涵蓋所有 API 測試情境
- ✅ 基本 README.md + DEPLOYMENT.md 文件完整

**Workshop Demo 可行性:**
- ✅ 演講者可用 Postman 執行 15-20 分鐘完整展示
- ✅ 展示「10-15 分鐘手動排查 vs 1-2 分鐘自動定位」的強烈對比
- ✅ 觀眾可理解 SAGA 可視化、Chaos 即時整合、完整 trace propagation 的價值

---
## Functional Requirements

### Banking Operations (銀行業務操作)

- **FR1:** 使用者可以查詢其所有帳戶資訊(包含帳戶 ID、幣別、餘額)
  - **驗收標準:**
    - AC1: 呼叫 GET /accounts,回傳 200 + 使用者所有帳戶列表
    - AC2: 回應包含每個帳戶的 account_id, currency, balance 欄位
    - AC3: 若使用者無帳戶,回傳空陣列 []

- **FR2:** 使用者可以向指定帳戶存款(指定金額與幣別)
  - **驗收標準:**
    - AC1: 呼叫 POST /deposits,回傳 200 + 更新後的帳戶餘額
    - AC2: Deposit-Withdrawal Service 呼叫 Transaction Service 更新餘額
    - AC3: Transaction Service 建立對應的交易記錄(transaction_type = DEPOSIT)

- **FR3:** 使用者可以從指定帳戶提款(指定金額與幣別)
  - **驗收標準:**
    - AC1: 呼叫 POST /withdrawals,正常提款時回傳 200 + 更新後的帳戶餘額
    - AC2: 餘額不足時,Transaction Service 拒絕並回傳 400 + 錯誤訊息 "Insufficient balance"
    - AC3: 提款金額 ≤ 0 時,回傳 400 + 錯誤訊息 "Invalid amount"
    - AC4: Transaction Service 建立對應的交易記錄(transaction_type = WITHDRAWAL)

- **FR4:** 使用者可以在同幣別帳戶間進行轉帳(指定來源帳戶、目標帳戶、金額)
  - **驗收標準:**
    - AC1: 呼叫 POST /transfers,正常轉帳時回傳 200 + 來源與目標帳戶的更新餘額
    - AC2: Transfer Service 呼叫 Account Service 驗證帳戶，再呼叫 Transaction Service 執行扣款和入帳
    - AC3: Transaction Service 建立兩筆交易記錄(TRANSFER_OUT 和 TRANSFER_IN)
    - AC4: 幣別不同時,回傳 400 + 錯誤訊息 "Currency mismatch, use exchange API"

- **FR5:** 使用者可以進行跨幣別換匯轉帳(指定來源帳戶/幣別、目標帳戶/幣別、金額)
  - **驗收標準:**
    - AC1: 呼叫 POST /exchanges,正常換匯時回傳 200 + 來源與目標帳戶的更新餘額 + 使用的匯率
    - AC2: Exchange Service 呼叫 Exchange Rate Service 查詢匯率，Account Service 驗證帳戶，Transaction Service 執行金流
    - AC3: Transaction Service 建立對應的交易記錄(transaction_type = EXCHANGE_OUT/EXCHANGE_IN)
    - AC4: 所有步驟在 Grafana Tempo 完整可視化

- **FR6:** 系統可以查詢即時匯率(Phase 1 使用模擬匯率數據)
  - **驗收標準:**
    - AC1: 呼叫 GET /rates/{from}/{to},回傳 200 + 匯率數值
    - AC2: Phase 1 回傳固定匯率(如 USD/TWD = 31.5)
    - AC3: 回應包含 rate, from_currency, to_currency, timestamp 欄位

- **FR7:** 系統可以在交易失敗時自動執行 SAGA 補償邏輯(解凍餘額、回滾交易記錄)
  - **驗收標準:**
    - AC1: 模擬 Exchange Service 失敗,系統自動執行 unfreeze balance 補償邏輯
    - AC2: 帳戶餘額恢復至交易前狀態
    - AC3: 交易記錄狀態更新為 COMPENSATED
    - AC4: Grafana Tempo 中可見 compensation spans (unfreeze-balance, rollback-transaction)

- **FR8:** 系統可以確保所有交易操作具備冪等性(使用唯一 transaction_id 防止重複扣款)
  - **驗收標準:**
    - AC1: 使用相同 transaction_id 重複呼叫轉帳 API,第二次回傳 409 Conflict
    - AC2: 資料庫中相同 transaction_id 僅存在一筆記錄
    - AC3: 帳戶餘額不會重複扣款

---

### Transaction & History Management (交易與歷史管理)

- **FR9:** 使用者可以查詢指定帳戶的完整交易歷史記錄
  - **驗收標準:**
    - AC1: 呼叫 GET /transactions/history?accountId={id},回傳 200 + 交易記錄陣列
    - AC2: 回應包含該帳戶所有交易(作為 from 或 to 的交易)
    - AC3: 交易記錄依 created_at 降冪排序(最新在前)

- **FR10:** 使用者可以依日期範圍篩選交易記錄
  - **驗收標準:**
    - AC1: 呼叫 GET /transactions/history?accountId={id}&startDate=2025-01-01&endDate=2025-12-31,僅回傳該期間內的交易
    - AC2: 不提供 startDate/endDate 時,回傳所有交易
    - AC3: startDate > endDate 時,回傳 400 + 錯誤訊息

- **FR11:** 系統可以記錄每筆交易的完整資訊(transaction_id, trace_id, account_id_from/to, amount, currency, type, status, created_at, metadata)
  - **驗收標準:**
    - AC1: 執行任一交易操作後,transaction table 包含新記錄
    - AC2: 記錄包含所有必填欄位:transaction_id, trace_id, amount, currency, type, status, created_at
    - AC3: metadata 欄位為 JSON 格式,包含 SAGA 執行細節

- **FR12:** 系統可以確保交易記錄不可篡改(Append-Only Transaction Table,僅能新增不可修改或刪除)
  - **驗收標準:**
    - AC1: transaction table 無 UPDATE 或 DELETE 操作的 SQL 語句
    - AC2: 嘗試修改交易記錄時,應用程式層級拒絕操作
    - AC3: 資料庫 migration 腳本中 transaction table 無 ON UPDATE CASCADE 設定

- **FR13:** 系統可以記錄交易狀態變化(PENDING, COMPLETED, FAILED, COMPENSATED)
  - **驗收標準:**
    - AC1: 交易成功時,status = COMPLETED
    - AC2: 交易失敗且未觸發補償時,status = FAILED
    - AC3: 交易失敗且觸發 SAGA 補償時,status = COMPENSATED
    - AC4: status 欄位為 ENUM 類型,僅允許這 4 種值

- **FR14:** 系統可以在交易記錄的 metadata 欄位中保存 SAGA 補償邏輯執行記錄
  - **驗收標準:**
    - AC1: SAGA 補償執行後,metadata 包含 compensation_steps 欄位
    - AC2: compensation_steps 記錄執行的補償操作(如 ["unfreeze-balance", "rollback-transaction"])
    - AC3: metadata 包含 failure_reason 欄位說明失敗原因

- **FR15:** 系統可以在每筆交易記錄中儲存對應的 trace_id(供稽核與問題排查使用,無論該 trace 是否仍在 Grafana Tempo 保留期限內)
  - **驗收標準:**
    - AC1: 呼叫 POST /transactions/transfer,檢查 transaction table 包含 trace_id 欄位(VARCHAR 32)
    - AC2: 呼叫 GET /transactions/history,回應 JSON 中每筆交易包含 trace_id
    - AC3: 即使對應的 trace 已從 Grafana Tempo 刪除(超過 7 天),transaction table 仍保留 trace_id
    - AC4: trace_id 欄位建立索引,支援快速查詢

---

### OpenTelemetry Instrumentation (分散式追蹤實作)

- **FR16:** 系統可以配置 OpenTelemetry 採樣策略(Phase 1 使用 100% 採樣以確保 Workshop 每次展示都有完整 trace)
  - **驗收標準:**
    - AC1: OTLP Collector config.yaml 設定 processors.probabilistic_sampler.sampling_percentage = 100
    - AC2: 連續執行 100 次 API 呼叫,Grafana Tempo 查詢顯示 100 個 trace
    - AC3: 配置文件中註解說明:Production 環境建議調整為 1-10% 採樣

- **FR17:** 系統可以為每筆 API 請求生成唯一的 trace_id 並在整個系統中傳遞(從 API Gateway 到所有微服務到 Kafka)
  - **驗收標準:**
    - AC1: 呼叫任一 API,回應 headers 包含 X-Trace-Id
    - AC2: 在 Grafana Tempo 查詢該 trace_id,可見完整的服務調用鏈路
    - AC3: 同一筆請求的所有 span 共享相同的 trace_id

- **FR18:** 系統可以為每個業務操作建立 span 並記錄執行時間、狀態(OK/ERROR)、錯誤訊息
  - **驗收標準:**
    - AC1: Grafana Tempo 中每個 span 包含 duration, status 欄位
    - AC2: 正常執行時 span.status = OK
    - AC3: 發生錯誤時 span.status = ERROR + span.error_message 包含錯誤訊息

- **FR19:** 系統可以在 span attributes 中包含業務語義資訊(account.id, transaction.amount, exchange.rate, currency, transaction.type 等)
  - **驗收標準:**
    - AC1: 轉帳操作的 span 包含 attributes: account.id.from, account.id.to, transaction.amount, transaction.currency
    - AC2: 換匯操作的 span 包含 attributes: exchange.rate, exchange.from_currency, exchange.to_currency
    - AC3: 在 Grafana Tempo span details 中可見這些 attributes

- **FR20:** 系統可以正確設定 span 命名與父子層級結構,使 SAGA 6 個步驟(freeze-balance → query-exchange-rate → deduct → credit → record → notify)在 trace 視覺化工具中清楚呈現
  - **驗收標準:**
    - AC1: Grafana Tempo 顯示轉帳 trace 包含 6 個 child spans,命名清楚(freeze-balance, query-exchange-rate, deduct, credit, record, notify)
    - AC2: Span 層級結構正確:root span → 6 個 sequential child spans
    - AC3: 每個 span 的 parent_span_id 正確關聯

- **FR21:** 系統可以在 SAGA 補償邏輯執行時建立對應的 compensation spans(unfreeze-balance, rollback-transaction)
  - **驗收標準:**
    - AC1: 模擬 Exchange Service 失敗,Grafana Tempo 顯示 compensation spans
    - AC2: Compensation spans 命名清楚:unfreeze-balance, rollback-transaction
    - AC3: Compensation spans 的 parent_span_id 關聯至原始 SAGA root span

- **FR22:** 系統可以在所有 log 輸出中自動包含當前的 trace_id 與 span_id(供 log ↔ trace 關聯)
  - **驗收標準:**
    - AC1: 任一服務的 log 輸出包含 trace_id 與 span_id 欄位
    - AC2: 在 Grafana Loki 查詢 log,可見 trace_id 與 span_id
    - AC3: Log 格式為 JSON,包含 {"level": "INFO", "message": "...", "trace_id": "...", "span_id": "..."}

- **FR23:** 系統可以在 Kafka 訊息 headers 中正確傳遞 W3C TraceContext(traceparent, tracestate headers)
  - **驗收標準:**
    - AC1: Kafka Producer 發送訊息時,headers 包含 traceparent 與 tracestate
    - AC2: Kafka Consumer 接收訊息時,從 headers 讀取 traceparent 並建立 child span
    - AC3: 在 Grafana Tempo 中,Kafka Producer span 與 Consumer span 正確關聯(Consumer span 的 parent_span_id = Producer span_id)

- **FR24:** 系統可以確保跨語言 trace propagation 正確無斷鏈(Java SpringBoot → Java Quarkus → Kafka Producer → Kafka Consumer)
  - **驗收標準:**
    - AC1: 執行跨服務交易(涉及 SpringBoot Account Service → Quarkus Exchange Service → Kafka),Grafana Tempo 顯示完整 trace 無斷點
    - AC2: 所有 spans 共享相同 trace_id
    - AC3: Span 層級結構正確,無孤立 span
  - **⚠️ Fallback Plan:** 若 Week 2 驗證 Quarkus OpenTelemetry SDK 整合失敗,Exchange Service 改用 SpringBoot 實作

- **FR25:** 系統可以在所有 API 回應的 HTTP headers 中包含 X-Trace-Id(方便前端記錄與除錯)
  - **驗收標準:**
    - AC1: 呼叫任一 API,回應 headers 包含 X-Trace-Id
    - AC2: X-Trace-Id 值與 OpenTelemetry trace_id 一致
    - AC3: 前端可從 response.headers['X-Trace-Id'] 取得 trace_id

- **FR26:** 系統可以在 API 錯誤回應的 JSON body 中包含 trace_id 欄位
  - **驗收標準:**
    - AC1: 呼叫 API 觸發錯誤(如餘額不足),回應 JSON 包含 {"error_code": "...", "error_message": "...", "trace_id": "..."}
    - AC2: trace_id 值與 OpenTelemetry trace_id 一致
    - AC3: 所有 4xx 與 5xx 錯誤回應都包含 trace_id

---

### Grafana Stack Integration (可觀測性平台整合)

- **FR27:** 系統可以配置所有微服務將 traces 匯出至 Grafana Tempo(使用 OTLP exporter)
  - **驗收標準:**
    - AC1: 所有微服務的 application.yaml/properties 設定 OTLP exporter endpoint = tempo:4317
    - AC2: 執行任一交易,Grafana Tempo 可查詢到對應 trace
    - AC3: Tempo 接收到的 trace 包含所有微服務的 spans

- **FR28:** 系統可以配置所有微服務將 logs 匯出至 Grafana Loki(使用 Loki log appender)
  - **驗收標準:**
    - AC1: 所有微服務的 logback.xml 設定 Loki appender endpoint = loki:3100
    - AC2: 在 Grafana Loki 查詢,可見所有微服務的 logs
    - AC3: Log 包含 service_name label,可依服務篩選

- **FR29:** 系統可以配置所有微服務將 metrics 匯出至 Prometheus(使用 Prometheus exporter)
  - **驗收標準:**
    - AC1: 所有微服務暴露 /actuator/prometheus endpoint
    - AC2: Prometheus 成功 scrape 所有微服務的 metrics
    - AC3: Grafana 中可查詢到 http_server_requests_seconds 等 metrics

- **FR30:** 系統可以在 Grafana 中設定 Tempo ↔ Loki 資料源關聯(透過 trace_id 與 span_id 欄位)
  - **驗收標準:**
    - AC1: Grafana datasource 設定中,Tempo 的 "Derived fields" 關聯至 Loki
    - AC2: 在 Grafana Tempo trace 視圖中,點擊 "View Logs" 可跳轉至 Loki
    - AC3: Loki 查詢自動帶入 trace_id filter

- **FR31:** 系統可以在 Grafana 中設定 Prometheus exemplars 關聯至 Tempo traces
  - **驗收標準:**
    - AC1: Prometheus metrics 包含 exemplar (trace_id)
    - AC2: 在 Grafana Prometheus 圖表中,點擊 exemplar 標記可跳轉至 Tempo
    - AC3: Tempo 查詢自動帶入對應 trace_id

- **FR32:** 系統可以配置 Grafana Tempo 資料保留期限(Phase 1 建議 7 天,Production 環境建議 30-90 天依合規要求調整)
  - **驗收標準:**
    - AC1: Tempo config.yaml 設定 compactor.retention_period = 7d
    - AC2: Tempo compactor 正確運行(kubectl logs 顯示 compactor 執行日誌)
    - AC3: 配置文件註解說明 Production 環境建議值

- **FR33:** 系統可以配置 Grafana Loki 資料保留期限(Phase 1 建議 14 天,Production 環境建議 30-180 天)
  - **驗收標準:**
    - AC1: Loki config.yaml 設定 table_manager.retention_period = 14d + compactor 啟用
    - AC2: Loki compactor 正確運行
    - AC3: 配置文件註解說明 Production 環境建議值

---

### Observability Demonstration Capabilities (可觀測性展示能力)

- **FR34:** 演講者可以在 Grafana Tempo 中查詢指定 trace_id 的完整 trace(若該 trace 仍在保留期限內)
  - **驗收標準:**
    - AC1: 演講者在 Grafana Tempo 查詢介面輸入 trace_id,可顯示完整 trace
    - AC2: Trace 視圖包含所有 spans 與層級結構
    - AC3: 若 trace 已過期(超過 7 天),顯示 "Trace not found" 訊息

- **FR35:** 演講者可以在 Grafana Tempo trace 視圖中點擊「View Logs」跳轉至 Grafana Loki 對應時間範圍的 logs
  - **驗收標準:**
    - AC1: Tempo trace 視圖右上角有 "View Logs" 按鈕
    - AC2: 點擊後跳轉至 Loki,自動帶入 trace_id filter
    - AC3: Loki 查詢結果顯示該 trace 相關的 logs

- **FR36:** 演講者可以在 Grafana Loki logs 中看到每行 log 包含的 trace_id,並可點擊連結跳轉至 Grafana Tempo
  - **驗收標準:**
    - AC1: Loki logs 每行包含 trace_id 欄位(可視為文字或超連結)
    - AC2: 點擊 trace_id 連結,跳轉至 Tempo 查詢該 trace
    - AC3: 若 Loki 設定正確,trace_id 顯示為可點擊的藍色連結

- **FR37:** 演講者可以在 Grafana Prometheus metrics 圖表中點擊 exemplar 標記跳轉至對應的 Grafana Tempo trace
  - **驗收標準:**
    - AC1: Prometheus 圖表中顯示 exemplar 標記(小圓點)
    - AC2: 滑鼠移至 exemplar 顯示 trace_id tooltip
    - AC3: 點擊 exemplar 跳轉至 Tempo 查詢該 trace

- **FR38:** 演講者可以在 Grafana Tempo 中視覺化 SAGA 流程的 6 個步驟,並清楚看到每個 span 的執行時間與狀態
  - **驗收標準:**
    - AC1: Tempo trace 視圖顯示 6 個 spans:freeze-balance, query-exchange-rate, deduct, credit, record, notify
    - AC2: 每個 span 顯示執行時間(duration)與狀態(OK/ERROR)
    - AC3: Span 層級結構清晰,易於理解執行順序

- **FR39:** 演講者可以在 Grafana Tempo 中視覺化 SAGA 補償邏輯的執行過程(失敗情境下的 compensation spans)
  - **驗收標準:**
    - AC1: 模擬交易失敗,Tempo 顯示 compensation spans:unfreeze-balance, rollback-transaction
    - AC2: Compensation spans 標記為不同顏色或圖示(與正常 spans 區分)
    - AC3: Span attributes 包含 compensation=true 標記

- **FR40:** 演講者可以在 Grafana Tempo trace 中看到標記為 ERROR 的 span,並快速定位故障點
  - **驗收標準:**
    - AC1: 觸發故障情境(如 Exchange Service 延遲),Tempo 顯示對應 span 為紅色或 ERROR 標記
    - AC2: Span details 包含錯誤訊息與 stack trace
    - AC3: 演講者可在 1-2 分鐘內從 trace 視圖定位故障 span

---

### Chaos Engineering (混沌工程)

- **FR41:** 演講者可以透過 Chaos Mesh 注入網路延遲故障(NetworkChaos:指定服務延遲 N 秒)
  - **驗收標準:**
    - AC1: 執行 kubectl apply -f networkchaos-exchange-service.yaml,Chaos Mesh 成功建立 NetworkChaos 資源
    - AC2: kubectl get networkchaos 顯示 status=Running
    - AC3: 呼叫 Exchange Service API,回應時間 > N 秒(如設定 5 秒延遲,實際回應時間 > 5000ms)

- **FR42:** 演講者可以透過 Chaos Mesh 注入 Pod 終止故障(PodChaos:終止指定服務 Pod)
  - **驗收標準:**
    - AC1: 執行 kubectl apply -f podchaos-exchange-service.yaml,目標 Pod 被終止
    - AC2: Kubernetes 自動重啟新 Pod
    - AC3: 期間呼叫 API 回傳 503 Service Unavailable

- **FR43:** 演講者可以透過 Chaos Mesh 注入 CPU/記憶體壓力(StressChaos:指定資源使用率)
  - **驗收標準:**
    - AC1: 執行 kubectl apply -f stresschaos-account-service.yaml,目標 Pod CPU 使用率上升
    - AC2: kubectl top pod 顯示目標 Pod CPU/Memory 使用率達到設定值
    - AC3: API 回應時間增加(因資源受限)

- **FR44:** 系統可以在 Chaos 故障注入後,在 Grafana Tempo trace 中立即反映故障影響(延遲增加、ERROR status、timeout 錯誤訊息)
  - **驗收標準:**
    - AC1: NetworkChaos 注入後,Tempo 顯示 Exchange Service span duration > 5000ms
    - AC2: PodChaos 注入後,Tempo 顯示對應 span status=ERROR + 錯誤訊息 "Service Unavailable"
    - AC3: Chaos 發生時間與 trace timestamp 一致

- **FR45:** 系統可以確保 Chaos 故障注入可控且可恢復(故障清除後服務自動恢復正常)
  - **驗收標準:**
    - AC1: 執行 kubectl delete networkchaos exchange-delay,NetworkChaos 移除
    - AC2: 再次呼叫 Exchange Service API,回應時間恢復正常 < 500ms
    - AC3: 系統無需人工介入即可自動恢復

- **FR46:** 演講者可以在 Chaos 故障注入後 1-2 分鐘內,透過 Grafana Tempo 定位根因(哪個服務、哪個 span 發生問題)
  - **驗收標準:**
    - AC1: NetworkChaos 注入 5 秒延遲,演講者在 Tempo 中輸入 trace_id
    - AC2: 1-2 分鐘內從 trace 視圖看到 Exchange Service span duration = 5.2 秒(紅色標記)
    - AC3: Span details 顯示明確的延遲原因

---

### System Deployment & Operations (系統部署與維運)

- **FR47:** 開發者可以使用 Helm Charts 一鍵部署完整系統至 Kubernetes(包含所有微服務、Grafana Stack、Kafka、PostgreSQL、Chaos Mesh)
  - **驗收標準:**
    - AC1: 執行 helm install lite-bank-demo ./helm -n demo --create-namespace,所有 Pods 啟動成功
    - AC2: kubectl get pods -n demo 顯示所有 Pods status=Running
    - AC3: Helm Chart 包含:6 個微服務 + Grafana + Tempo + Loki + Prometheus + Kafka + PostgreSQL + Chaos Mesh

- **FR48:** 開發者可以使用 Docker Compose 在本地快速啟動系統(簡化版,不包含 Chaos Mesh)
  - **驗收標準:**
    - AC1: 執行 docker-compose up -d,所有容器啟動成功
    - AC2: docker-compose ps 顯示所有容器 status=Up
    - AC3: 本地可訪問 http://localhost:8080 (API Gateway) 與 http://localhost:3000 (Grafana)
    - AC4: Docker Compose 不包含 Chaos Mesh(Chaos Mesh 需要 Kubernetes)

- **FR49:** 系統可以在 Kubernetes 中為所有微服務正確設定 Liveness Probes(確保故障 Pod 自動重啟)
  - **驗收標準:**
    - AC1: 所有微服務的 Deployment YAML 包含 livenessProbe 設定
    - AC2: livenessProbe 指向 /actuator/health/liveness endpoint
    - AC3: 模擬服務故障(kill process),Kubernetes 自動重啟 Pod

- **FR50:** 系統可以在 Kubernetes 中為所有微服務正確設定 Readiness Probes(確保未就緒 Pod 不接收流量)
  - **驗收標準:**
    - AC1: 所有微服務的 Deployment YAML 包含 readinessProbe 設定
    - AC2: readinessProbe 指向 /actuator/health/readiness endpoint
    - AC3: 服務啟動時,未就緒前不接收流量(kubectl describe pod 顯示 Ready=0/1)

- **FR51:** 開發者可以透過 Helm values.yaml 管理不同環境設定(dev, staging, prod 環境變數差異)
  - **驗收標準:**
    - AC1: Helm Chart 包含 values-dev.yaml, values-staging.yaml, values-prod.yaml
    - AC2: 執行 helm install -f values-dev.yaml,使用 dev 環境設定(如 DB 連線字串、Kafka bootstrap servers)
    - AC3: values.yaml 支援覆蓋所有環境參數

- **FR52:** 系統可以提供基本 README.md 文件(快速開始指南,目標 5 分鐘內啟動系統)
  - **驗收標準:**
    - AC1: README.md 包含快速開始步驟(Prerequisites, Installation, Quick Start)
    - AC2: 依照 README.md 步驟,可在 5 分鐘內啟動系統(Helm 或 Docker Compose)
    - AC3: README.md 包含基本架構圖與系統元件說明

- **FR53:** 系統可以提供基本 DEPLOYMENT.md 文件(詳細部署步驟、環境需求清單、基本故障排查指南)
  - **驗收標準:**
    - AC1: DEPLOYMENT.md 包含詳細部署步驟(Helm Chart 參數說明、環境變數清單)
    - AC2: 包含環境需求:Kubernetes 版本、Helm 版本、kubectl 版本
    - AC3: 包含基本故障排查指南(如 Pod 啟動失敗、Grafana 無法連線至 Tempo)

---

### Authentication & Security (認證與安全)

- **FR54:** 使用者可以使用帳號密碼登入系統(POST /auth/login,返回 JWT token)
  - **驗收標準:**
    - AC1: 呼叫 POST /auth/login,正確帳密回傳 200 + {"token": "...", "expires_in": 3600}
    - AC2: 錯誤帳密回傳 401 + 錯誤訊息 "Invalid credentials"
    - AC3: JWT token 包含 user_id, account_ids claims

- **FR55:** 系統可以在 API Gateway 層級驗證所有 API 請求的 JWT token 有效性
  - **驗收標準:**
    - AC1: 無 JWT token 呼叫受保護 API,回傳 401 Unauthorized
    - AC2: 過期 JWT token 呼叫 API,回傳 401 + 錯誤訊息 "Token expired"
    - AC3: 有效 JWT token 呼叫 API,正常回傳 200

- **FR56:** 系統可以在 JWT token 中包含使用者與帳戶資訊(user_id, account_ids claims)
  - **驗收標準:**
    - AC1: 解碼 JWT token,payload 包含 user_id, account_ids 欄位
    - AC2: account_ids 為陣列,包含該使用者所有帳戶 ID
    - AC3: API 可從 JWT token 取得 user_id,無需額外查詢資料庫

- **FR57:** 系統可以使用 bcrypt 或 Argon2 演算法 hash 儲存使用者密碼
  - **驗收標準:**
    - AC1: 檢查 user table,password 欄位為 hash 值(非明文)
    - AC2: Hash 演算法為 bcrypt 或 Argon2(檢查程式碼)
    - AC3: 登入時使用 hash 比對,不使用明文比對

- **FR58:** 系統可以透過 Kubernetes Ingress 設定 HTTPS/TLS 加密所有 API 通訊
  - **驗收標準:**
    - AC1: Ingress YAML 包含 tls 設定與憑證
    - AC2: 訪問 https://api.lite-bank-demo.local,瀏覽器顯示安全連線
    - AC3: HTTP 請求自動重定向至 HTTPS

---

### API Contract & Testing (Phase 1.5 準備)

- **FR66:** 所有 API Response 使用統一格式(包含 success, data, trace_id, timestamp)
  - **驗收標準:**
    - AC1: 所有成功回應格式為 {"success": true, "data": {...}, "trace_id": "...", "timestamp": "2025-12-15T10:00:00Z"}
    - AC2: 所有微服務遵循相同的 Response wrapper
    - AC3: Postman Collection 中所有範例 Response 包含這些欄位

- **FR67:** 所有 API Error Response 使用統一格式(包含 error_code, error_message, trace_id)
  - **驗收標準:**
    - AC1: 所有錯誤回應格式為 {"success": false, "error_code": "INSUFFICIENT_BALANCE", "error_message": "餘額不足", "trace_id": "...", "timestamp": "..."}
    - AC2: error_code 為標準化代碼(如 INSUFFICIENT_BALANCE, INVALID_AMOUNT, SERVICE_UNAVAILABLE)
    - AC3: Postman Collection 包含錯誤情境範例

- **FR68:** 系統可以配置 API 支援 CORS 設定(允許 localhost:3000 前端開發呼叫 API)
  - **驗收標準:**
    - AC1: API Gateway 設定 CORS allowed origins = http://localhost:3000
    - AC2: 前端開發時可從 localhost:3000 成功呼叫 API
    - AC3: CORS 允許 credentials (withCredentials=true)

- **FR69:** 系統可以提供 Postman Collection 涵蓋所有 API 測試案例(包含正常情境與錯誤情境)
  - **驗收標準:**
    - AC1: Postman Collection 包含所有 API endpoints (存款、提款、轉帳、換匯、交易查詢、登入)
    - AC2: 包含錯誤情境測試(餘額不足、無效金額、未授權)
    - AC3: Collection 包含預設測試資料(測試帳戶、JWT token)

- **FR70:** 系統可以提供 Integration Test 涵蓋 SAGA 補償邏輯測試
  - **驗收標準:**
    - AC1: Integration Test 模擬 Exchange Service 失敗,驗證補償邏輯執行
    - AC2: 測試通過標準:帳戶餘額恢復原狀 + transaction status = COMPENSATED
    - AC3: 測試覆蓋率 > 80%(針對 SAGA 相關程式碼)

- **FR71:** 系統可以提供 Smoke Test 驗證 Helm 部署成功(驗證所有服務正常啟動與基本功能可用)
  - **驗收標準:**
    - AC1: Smoke Test 檢查所有 Pods status=Running
    - AC2: 測試所有 /health/liveness endpoints 回傳 200
    - AC3: 執行一次完整交易流程(登入 → 轉帳 → 查詢交易歷史),驗證基本功能正常

---

## Functional Requirements Summary

**總計:** 64 項功能需求 (Phase 1 範圍)

**能力領域覆蓋:**
- **Banking Operations** (銀行業務操作): 8 項 (FR1-FR8)
- **Transaction & History Management** (交易與歷史管理): 7 項 (FR9-FR15)
- **OpenTelemetry Instrumentation** (分散式追蹤實作): 11 項 (FR16-FR26)
- **Grafana Stack Integration** (可觀測性平台整合): 7 項 (FR27-FR33)
- **Observability Demonstration Capabilities** (可觀測性展示能力): 7 項 (FR34-FR40)
- **Chaos Engineering** (混沌工程): 6 項 (FR41-FR46)
- **System Deployment & Operations** (系統部署與維運): 7 項 (FR47-FR53)
- **Authentication & Security** (認證與安全): 5 項 (FR54-FR58)
- **API Contract & Testing** (API 契約與測試): 6 項 (FR66-FR71)

**Phase 2 延後項目:**
- Workshop Materials (原 FR59-65,共 7 項) → 移至 Phase 2

**關鍵改進 (專家驗證修正):**
1. ✅ **FR15:** 明確區分「系統永久保存 trace_id」vs「Tempo 有期限保存 trace」
2. ✅ **FR16-FR33:** 重新組織為「OpenTelemetry Instrumentation」+「Grafana Stack Integration」+「Observability Demonstration」三大類別
3. ✅ **FR66-FR71:** 新增 API Contract & Testing 類別(為 Phase 1.5 前端與 QA 準備)
4. ✅ **所有 FR 補充驗收標準 (Acceptance Criteria)**,確保可測試性
5. ✅ **FR24 加註 Fallback Plan:**若 Quarkus OpenTelemetry SDK 整合失敗,改用 SpringBoot

**這份 FR 清單已通過 6 位專家驗證 (Architect, SRE, PM, UX Designer, QA, Tech Lead),可作為 Phase 1 開發的能力契約。**

---
## Non-Functional Requirements

### Performance (效能需求)

**NFR-P1: API 回應時間標準**
- **需求:** 所有 API 端點在正常情境下,P95 回應時間 < 500ms
- **測試方式:** 使用 Postman Collection 執行 100 次呼叫,統計 P95 回應時間
- **驗收標準:** 95% 的 API 呼叫在 500ms 內完成
- **理由:** Workshop 展示需要流暢體驗,過長的回應時間影響觀眾信心

**NFR-P2: SAGA 交易處理時間**
- **需求:** 正常轉帳/換匯交易(包含 SAGA 6 個步驟)在 2 秒內完成
- **測試方式:** 執行轉帳 API,測量從請求到回應的總時間
- **驗收標準:** 正常情境下,交易處理時間 < 2000ms
- **理由:** 使用者(演講者/觀眾)對交易回應時間有明確期望

**NFR-P3: Chaos 注入後的效能降級可控**
- **需求:** NetworkChaos 注入 5 秒延遲時,僅影響目標服務,其他服務回應時間 < 1 秒
- **測試方式:** 注入 NetworkChaos 至 Exchange Service,呼叫 Account Service API,測量回應時間
- **驗收標準:** 非目標服務回應時間增幅 < 50%
- **理由:** Chaos 展示需要精確控制故障範圍,避免整體系統癱瘓

**NFR-P4: Grafana 查詢回應時間**
- **需求:** Grafana Tempo 查詢 trace_id 回應時間 < 3 秒
- **測試方式:** 在 Grafana Tempo UI 輸入 trace_id,測量查詢結果顯示時間
- **驗收標準:** 查詢回應時間 < 3000ms (P95)
- **理由:** Workshop 演講者需要快速展示 trace 定位效果,過長查詢時間影響展示節奏

---

### Observability (可觀測性品質) - 核心 NFR

**NFR-O1: Trace 完整性標準**
- **需求:** 所有交易操作產生完整的 trace,包含所有微服務 spans,無斷鏈
- **測試方式:** 執行 100 次交易,檢查 Grafana Tempo 中每個 trace 包含預期的所有 spans
- **驗收標準:** 100% 的 trace 包含完整的服務調用鏈路（協調層 → Account Service → Transaction Service）
- **理由:** 這是專案的核心價值,trace 不完整將無法展示可觀測性效果

**NFR-O2: Span Attributes 業務語義完整性**
- **需求:** 所有交易相關 spans 包含業務語義 attributes (account.id, transaction.amount, exchange.rate, currency 等)
- **測試方式:** 檢查 Grafana Tempo 中 span details,驗證 attributes 存在且正確
- **驗收標準:** 所有交易 spans 包含至少 3 個業務語義 attributes
- **理由:** 業務語義資訊是快速定位問題的關鍵,缺少則無法理解業務流程

**NFR-O3: Log 結構化與可讀性標準**
- **需求:** 所有服務 logs 為結構化 JSON 格式,包含 level, message, trace_id, span_id, timestamp, service_name 欄位
- **測試方式:** 在 Grafana Loki 查詢 logs,驗證格式正確性
- **驗收標準:** 100% 的 logs 符合 JSON 格式且包含必填欄位
- **理由:** 結構化 logs 是 log ↔ trace 跳轉的基礎,格式不一致將無法正確關聯

**NFR-O4: Metrics 收集頻率與準確度**
- **需求:** Prometheus 每 15 秒 scrape 一次 metrics,http_server_requests_seconds 準確反映實際回應時間
- **測試方式:** 檢查 Prometheus targets 狀態,比對 metrics 與實際 API 回應時間
- **驗收標準:** Metrics 與實際測量值誤差 < 10%
- **理由:** Metrics 準確度影響 metric → trace 跳轉的有效性

**NFR-O5: 三向跳轉功能可用性**
- **需求:** log ↔ trace、trace ↔ log、metric → trace 三向跳轉在 Grafana 中 100% 可用
- **測試方式:** 手動測試 3 種跳轉路徑,驗證跳轉後顯示正確內容
- **驗收標準:** 3 種跳轉路徑全部成功,跳轉後顯示對應的 trace/log/metric
- **理由:** 這是 Workshop 展示的核心功能,任一跳轉失敗都會影響展示效果

**NFR-O6: SAGA 可視化清晰度**
- **需求:** SAGA 6 個步驟在 Grafana Tempo 中清楚呈現,span 命名一致且易於理解
- **測試方式:** 觀眾(非開發人員)能在 2 分鐘內從 Tempo trace 視圖理解 SAGA 流程
- **驗收標準:** Span 命名遵循一致格式(如 freeze-balance, query-exchange-rate),span 層級結構清晰
- **理由:** 可視化清晰度直接影響觀眾對 SAGA 模式的理解

**NFR-O7: Chaos 故障在 Trace 中的可見性**
- **需求:** Chaos 注入的故障(延遲、錯誤)在 Grafana Tempo trace 中立即可見且明顯標記
- **測試方式:** 注入 NetworkChaos,檢查 Tempo 中對應 span 是否顯示異常(duration 增加、status=ERROR、紅色標記)
- **驗收標準:** Chaos 故障在 trace 中以視覺化方式明顯呈現(紅色 span、ERROR status、duration > 5000ms)
- **理由:** 這是展示「1-2 分鐘快速定位」的關鍵,故障不明顯則無法展示效果

---

### Reliability (可靠性需求)

**NFR-R1: Workshop 期間服務可用性**
- **需求:** 在 Workshop 演講期間(20 分鐘),所有微服務可用性 > 99%
- **測試方式:** Workshop 預演時,所有服務持續運行 20 分鐘,無非預期重啟或故障
- **驗收標準:** 20 分鐘內無服務意外停機或重啟
- **理由:** Workshop 現場 Demo 失敗會嚴重影響技術信任度

**NFR-R2: SAGA 補償邏輯正確性**
- **需求:** 交易失敗時,SAGA 補償邏輯 100% 正確執行,帳戶餘額恢復至交易前狀態
- **測試方式:** 模擬 Exchange Service 失敗 100 次,驗證補償邏輯執行結果
- **驗收標準:** 100% 的失敗交易觸發補償邏輯,餘額正確恢復,transaction status = COMPENSATED
- **理由:** SAGA 補償邏輯是核心展示內容,任何錯誤都會質疑架構正確性

**NFR-R3: Chaos 故障恢復時間**
- **需求:** Chaos 故障清除後,系統在 30 秒內自動恢復正常
- **測試方式:** 注入 NetworkChaos,30 秒後清除,驗證 API 回應時間恢復正常
- **驗收標準:** 故障清除後 30 秒內,API 回應時間 < 500ms
- **理由:** Workshop 展示需要快速切換場景,恢復時間過長影響展示節奏

**NFR-R4: Database Transaction Integrity (資料庫交易完整性)**
- **需求:** 所有資料庫操作具備 ACID 特性,交易記錄不可篡改
- **測試方式:** 檢查 transaction table 無 UPDATE/DELETE 操作,併發交易測試無髒讀/幻讀
- **驗收標準:** 100 次併發交易測試,所有交易記錄一致且無資料遺失
- **理由:** 展示 Fintech 合規要求(Append-Only Transaction Table)

---

### Deployment (部署性需求)

**NFR-D1: 本地部署時間標準**
- **需求:** 觀眾依照 README.md 步驟,可在 15 分鐘內完成本地部署(Helm 或 Docker Compose)
- **測試方式:** 請 3 位未接觸過專案的工程師依文件部署,測量時間
- **驗收標準:** 平均部署時間 < 15 分鐘,所有 Pods/Containers 成功啟動
- **理由:** Workshop 結束後觀眾想自行嘗試,部署時間過長會降低學習意願

**NFR-D2: Helm 部署成功率**
- **需求:** Helm install 一次成功率 > 95%,無需人工介入修復
- **測試方式:** 在乾淨的 K8s 環境執行 helm install 20 次,統計成功次數
- **驗收標準:** 20 次部署中至少 19 次成功(所有 Pods Running)
- **理由:** 部署失敗會阻礙觀眾學習,影響專案推廣效果

**NFR-D3: Docker Compose 啟動穩定性**
- **需求:** Docker Compose up 後,所有容器在 3 分鐘內啟動完成且健康
- **測試方式:** 執行 docker-compose up -d,檢查 3 分鐘後所有容器 status=Up
- **驗收標準:** 所有容器在 3 分鐘內啟動成功,health check 通過
- **理由:** 本地快速驗證是重要使用情境,啟動時間過長影響體驗

**NFR-D4: 文件完整性與可理解性**
- **需求:** README.md 與 DEPLOYMENT.md 文件讓初次接觸者能獨立完成部署,無需額外支援
- **測試方式:** 請 3 位非專案成員依文件部署,記錄遇到的問題與疑問
- **驗收標準:** 80% 的測試者能獨立完成部署,無需詢問開發團隊
- **理由:** Workshop 無後續支援機制,文件必須足夠清楚

**NFR-D5: 環境相容性範圍**
- **需求:** 支援主流 Kubernetes 版本 **(1.32-1.34)** 與 Docker Compose v2
- **測試方式:** 在 K8s 1.32, 1.33, 1.34 與 Docker Compose v2 環境測試部署
- **驗收標準:** 所有測試環境部署成功
- **理由:** 觀眾環境多樣,相容性差會增加部署失敗率
- **版本更新說明 (2025-12-15):**
  - Kubernetes v1.34 為當前最新穩定版本(2025-08-27 發布)
  - Kubernetes v1.35 預計於 2025-12-17 發布
  - Phase 1 測試 1.32-1.34,Phase 1.5 應加入 v1.35 相容性驗證
  - 參考資料: [Kubernetes Releases](https://kubernetes.io/releases/), [K8s v1.34 Release Blog](https://kubernetes.io/blog/2025/08/27/kubernetes-v1-34-release/), [K8s Version Support Policy](https://endoflife.date/kubernetes)

---

### Maintainability (可維護性需求) - Phase 1 最小化

**NFR-M1: 程式碼可讀性標準**
- **需求:** 所有程式碼遵循一致的 coding style,關鍵業務邏輯有註解說明
- **測試方式:** Code review 檢查,SonarQube 靜態分析
- **驗收標準:** SonarQube 評分 > B,無 critical code smells
- **理由:** 觀眾可能會閱讀程式碼學習,程式碼可讀性影響學習效果

**NFR-M2: 配置外部化**
- **需求:** 所有環境相關設定(DB 連線、Kafka endpoint、Tempo endpoint)可透過環境變數或 values.yaml 覆蓋
- **測試方式:** 修改 values.yaml,驗證服務讀取新設定
- **驗收標準:** 無需修改程式碼即可切換環境設定
- **理由:** 觀眾部署時可能需要調整設定,硬編碼會增加修改難度

---

## Non-Functional Requirements Summary

**總計:** 17 項非功能需求

**NFR 類別覆蓋:**
- **Performance** (效能需求): 4 項 (NFR-P1 ~ NFR-P4)
- **Observability** (可觀測性品質): 7 項 (NFR-O1 ~ NFR-O7) ⭐ 核心類別
- **Reliability** (可靠性需求): 4 項 (NFR-R1 ~ NFR-R4)
- **Deployment** (部署性需求): 5 項 (NFR-D1 ~ NFR-D5)
- **Maintainability** (可維護性需求): 2 項 (NFR-M1 ~ NFR-M2)

**刻意跳過的類別 (對此專案不適用):**
- ❌ **Scalability** (擴展性): Demo 環境無高併發需求
- ❌ **Accessibility** (無障礙): Phase 1 無前端 UI
- ❌ **Usability** (易用性): Phase 1 僅 Postman API 測試
- ❌ **Compliance** (合規性): 已在 Domain Requirements (Fintech) 處理

**關鍵特點:**
1. ✅ **Observability 為核心 NFR 類別** (7 項需求,涵蓋 trace/log/metric 品質標準)
2. ✅ **所有 NFR 均可測試與量化** (具體數值、百分比、時間標準)
3. ✅ **聚焦 Workshop Demo 需求** (展示流暢度、快速部署、可靠性)
4. ✅ **Phase 1 最小化原則** (跳過不相關類別,避免需求膨脹)
5. ✅ **基於最新技術版本** (Kubernetes 1.32-1.34,2025-12-15 更新)

**文件版本追蹤:**
- 最後更新: 2025-12-15
- Kubernetes 版本參考: v1.34 (當前穩定), v1.35 (2025-12-17 即將發布)
- 建議 Phase 1.5 加入 v1.35 相容性測試

---
