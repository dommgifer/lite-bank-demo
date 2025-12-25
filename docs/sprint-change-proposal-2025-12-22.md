# Sprint Change Proposal - Swagger UI Implementation

**Date:** 2025-12-22
**Author:** PM Agent (John)
**Trigger:** Architecture Decision 3.2 未在 Epics 中實作
**Severity:** Minor
**Status:** Pending Approval

---

## 1. Issue Summary

### Problem Statement
在執行 E2E 測試期間，發現所有 7 個微服務（user-service, account-service, transaction-service, transfer-service, exchange-rate-service, exchange-service, deposit-withdrawal-service）均未實作 Swagger UI / OpenAPI Documentation 功能。

### Discovery Context
- **When:** 2025-12-22，執行 API Gateway E2E 測試時
- **How:** 嘗試訪問 `/swagger-ui.html` 和 `/v3/api-docs` 端點失敗
- **Who Discovered:** Dev Agent 在測試階段發現

### Evidence
1. **Architecture.md Section 3.2** 明確規定：
   ```
   Decision: OpenAPI 3.0 + Swagger UI
   Implementation:
   - 每個微服務提供 OpenAPI 規格檔(YAML 或 JSON)
   - Swagger UI endpoint: `/api-docs` 或 `/swagger-ui`
   - SpringBoot: 使用 `springdoc-openapi-starter-webmvc-ui` 依賴
   ```

2. **實際狀態檢查：**
   ```bash
   curl http://localhost:8080/swagger-ui.html
   # 結果: {"success":false,"error":{"code":"ERR_SYS_001",...}}

   grep -r "springdoc\|swagger" services/*/pom.xml
   # 結果: 0 筆符合
   ```

3. **epics.md 檢查：**
   - Epic 1-6 均未包含 Swagger UI 相關 story
   - Epic 6 (Documentation & Deployment) 也未涵蓋 API Documentation

### Root Cause Analysis
**主因:** PM Agent 在將 Architecture 決策轉換為 Epics/Stories 時，將 API Documentation 誤判為「技術細節」而非「可交付功能」，導致此需求被遺漏。

**次因:** PRD 中未將 API Documentation 列為明確的功能需求（FR），僅在 Architecture 決策層級提及。

---

## 2. Impact Analysis

### Epic Impact
**受影響 Epic:**
- **Epic 1: Foundation & Core Infrastructure Setup** ⚠️ 需要新增 Story
- **Epic 6: Documentation & Deployment Automation** ℹ️ 概念上應該包含，但實際未受影響

**影響程度:** Minor
- 不影響現有已完成的 stories
- 不需要修改已部署的功能
- 僅需增量添加新功能

### Story Impact
**新增 Story:**
- **Story 1.13: Add Swagger UI/OpenAPI Documentation to All Microservices**
  - 位置: Epic 1，Story 1.12 之後
  - 依賴: Story 1.10, 1.11, 1.12（已完成）
  - 影響: 7 個微服務需要加入依賴和配置

**現有 Stories:** 無需修改

### Artifact Conflicts

**需要更新的文件:**
1. ✅ **epics.md**
   - 在 Epic 1 Section 加入 Story 1.13
   - 更新 Epic 1 story 數量 (~12 → ~13)
   - 更新 FR Coverage Matrix

2. ❌ **prd.md** - 無需更新
   - API Documentation 屬於技術實作決策
   - 非使用者面向功能需求

3. ❌ **architecture.md** - 無需更新
   - 原本的決策已經正確且完整

### Technical Impact

**程式碼變更:**
- **影響檔案數:** 14 個檔案
  - 7 個 `pom.xml` (加入依賴)
  - 7 個 `application.yml` (Swagger 配置)
- **Controller 修改:** 可選（建議加入基本 annotations）
- **測試影響:** 無破壞性變更，建議加入 Swagger endpoint 測試

**部署影響:**
- ✅ 需要重新 build 所有微服務 Docker images
- ✅ 需要重啟所有微服務容器
- ⚠️ 估計停機時間: 5-10 分鐘（滾動重啟）

**基礎設施影響:**
- ❌ 無需新增資源（Swagger UI 內嵌在服務中）
- ❌ 無需調整 Kubernetes 配置

---

## 3. Recommended Approach

### Chosen Path: Direct Adjustment (直接調整)

**理由:**
1. **低風險:** 僅新增功能，不修改現有邏輯
2. **明確範圍:** 7 個服務，每個約 30-40 分鐘
3. **無依賴衝突:** Story 依賴已滿足
4. **即時價值:** 立即改善開發者體驗

### Alternative Paths Considered

❌ **Potential Rollback (潛在回滾):**
- 不適用 - 無已完成工作需要回滾

❌ **MVP Review (範疇重審):**
- 不適用 - API Documentation 屬於基礎設施，不影響 MVP 範疇

### Effort Estimate
**Total Effort:** 4-6 小時

**分解:**
- 每個服務實作: 30-40 分鐘 × 7 = 3.5-4.5 小時
- 測試與驗證: 1 小時
- 文件更新: 30 分鐘

### Risk Assessment
**Overall Risk:** 🟢 Low

**潛在風險:**
1. **依賴衝突:** 🟢 Low - springdoc 2.3.0 與 Spring Boot 3.4.1 完全相容
2. **效能影響:** 🟢 Low - Swagger UI 僅在開發/測試環境啟用
3. **安全風險:** 🟡 Medium - 需確保生產環境關閉 Swagger UI（透過 profile 控制）

**風險緩解措施:**
- 使用 `@Profile("dev")` 限制 Swagger 僅在開發環境啟用
- 在 `application-prod.yml` 明確停用 springdoc

### Timeline Impact
**Sprint 影響:** 最小

- 可在當前 Sprint 完成（若已在 Epic 1 階段）
- 或在下一個 Sprint 開始前補充（若已進入 Epic 2+）
- 不影響 Epic 2-6 的排程

---

## 4. Detailed Change Proposals

### Change Proposal #1: Add Story 1.13 to epics.md

**Document:** `docs/epics.md`
**Location:** Epic 1, after Story 1.12 (line ~588)

**Change Type:** INSERT

**NEW CONTENT:**
```markdown
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

**Prerequisites:**
- Story 1.10 (User Service) 完成
- Story 1.11 (API Gateway) 完成
- Story 1.12 (Unified API Response) 完成

**Estimated Effort:** 4-6 hours
- 每個服務約 30-40 分鐘
- 包含測試和文件驗證時間

**FR Coverage:** Architecture Decision 3.2 (API Documentation)
```

**Justification:**
- 補足 Architecture.md Section 3.2 的實作需求
- 提供開發者和測試人員必要的 API 文件工具
- 符合現代微服務最佳實踐

---

### Change Proposal #2: Update Epic 1 Story Count

**Document:** `docs/epics.md`
**Location:** Epic 1 header section (line ~113)

**Change Type:** EDIT

**BEFORE:**
```markdown
### Epic 1: Foundation & Core Infrastructure Setup

**Epic Goal:** 建立完整的技術基礎設施，包含資料庫、訊息佇列、可觀測性平台、認證系統，使所有後續使用者功能可以運作。

**Story 數量：** ~12 stories
```

**AFTER:**
```markdown
### Epic 1: Foundation & Core Infrastructure Setup

**Epic Goal:** 建立完整的技術基礎設施，包含資料庫、訊息佇列、可觀測性平台、認證系統、API 文件化工具，使所有後續使用者功能可以運作。

**Story 數量：** ~13 stories
```

**Justification:**
- 更新 story 總數以反映新增的 Story 1.13
- 在 Epic Goal 中加入「API 文件化工具」明確說明範疇

---

### Change Proposal #3: Update FR Coverage Matrix (Optional)

**Document:** `docs/epics.md`
**Location:** FR Coverage Matrix - Epic 1 section (line ~708)

**Change Type:** EDIT (Optional - 如果 Matrix 有詳細列表)

**BEFORE:**
```markdown
### Epic 1: Foundation & Core Infrastructure (12 Stories)
...
```

**AFTER:**
```markdown
### Epic 1: Foundation & Core Infrastructure (13 Stories)
...
- Story 1.13: Add Swagger UI/OpenAPI Documentation → Architecture Decision 3.2
```

**Justification:**
- 確保 FR Coverage Matrix 完整反映所有 stories
- 追蹤 Architecture Decision 的實作狀態

---

## 5. Implementation Handoff

### Change Scope Classification
**Category:** 🟡 Minor (可由開發團隊直接實作)

**理由:**
- 清晰的技術需求和 AC
- 無架構層級變更
- 低風險增量功能
- 已有明確的 Architecture 指引

### Handoff Recipients

**Primary:** 🔧 Dev Agent (Development Team)
- **Responsibility:** 實作 Story 1.13
- **Timeline:** 當前或下一個 Sprint
- **Deliverables:**
  - 7 個微服務均可訪問 Swagger UI
  - 通過所有 Acceptance Criteria
  - Pull Request 包含測試證明

**Secondary:** 📋 PM Agent (Product Manager - 就是我)
- **Responsibility:** 更新 epics.md 文件
- **Timeline:** 立即
- **Deliverables:**
  - Story 1.13 正式加入 epics.md
  - Epic 1 story count 更新
  - FR Coverage Matrix 更新

**Supporting:** 🏗️ SM Agent (Scrum Master - Optional)
- **Responsibility:** 將 Story 1.13 加入 Sprint Backlog
- **Timeline:** 下一次 Sprint Planning
- **Action Items:**
  - 評估 story points (建議: 3-5 points)
  - 安排到適當的 Sprint
  - 確保與其他 stories 無衝突

### Success Criteria

**完成定義 (Definition of Done):**
1. ✅ Story 1.13 已加入 epics.md
2. ✅ 所有 7 個微服務的 pom.xml 包含 springdoc 依賴
3. ✅ 所有微服務的 Swagger UI 可訪問 (`/swagger-ui.html`)
4. ✅ 所有微服務的 OpenAPI JSON 可下載 (`/v3/api-docs`)
5. ✅ 至少 user-service 和 account-service 的 Controller 有基本 annotations
6. ✅ Docker images 重新 build 並部署
7. ✅ 手動測試驗證所有 AC
8. ✅ 更新專案 README 說明如何訪問 Swagger UI

### Next Actions (Immediate)

**For PM Agent (我):**
1. [ ] 取得主人批准此 Sprint Change Proposal
2. [ ] 更新 `docs/epics.md` 加入 Story 1.13
3. [ ] 通知開發團隊新 story 已就緒

**For Dev Agent:**
1. [ ] 等待 Story 1.13 正式加入 backlog
2. [ ] 開始實作（或排程到下一個 Sprint）
3. [ ] 提交 Pull Request

**For 主人:**
1. [ ] 審閱並批准此 Sprint Change Proposal
2. [ ] 決定是否立即執行或排程到未來 Sprint

---

## 6. Appendix

### Related Documents
- `docs/architecture.md` - Section 3.2 (API Documentation)
- `docs/epics.md` - Epic 1: Foundation & Core Infrastructure Setup
- `docs/prd.md` - Project Requirements Document

### Reference Links
- SpringDoc OpenAPI Documentation: https://springdoc.org/
- OpenAPI 3.0 Specification: https://spec.openapis.org/oas/v3.0.0
- Swagger UI: https://swagger.io/tools/swagger-ui/

### Change Log
- 2025-12-22: Initial Sprint Change Proposal created by PM Agent
- 2025-12-22: Change Proposal #1 approved by 主人 (Incremental mode)

---

**Document Status:** ✅ Ready for Approval
**Next Step:** 等待主人最終批准，然後更新 epics.md 並交付給開發團隊
