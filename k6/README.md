# Lite Bank k6 流量模擬

模擬多用戶使用銀行平台，產生 traces 和 metrics。

## 安裝 k6

```bash
# macOS
brew install k6

# 或使用 Docker
docker pull grafana/k6
```

## 使用方式

### 基本執行（10 VUs，5 分鐘）

```bash
cd k6
k6 run traffic.js
```

### 自訂參數

```bash
# 5 個虛擬用戶，跑 2 分鐘
k6 run -e VUS=5 -e DURATION=2m traffic.js

# 20 個虛擬用戶，跑 10 分鐘
k6 run -e VUS=20 -e DURATION=10m traffic.js

# 指定不同的 API Gateway URL
k6 run -e BASE_URL=http://localhost:9000 traffic.js
```

### 使用 Docker 執行

```bash
docker run --rm -i --network=host grafana/k6 run - < traffic.js

# 或指定參數
docker run --rm -i --network=host \
  -e VUS=10 \
  -e DURATION=5m \
  grafana/k6 run - < traffic.js
```

## 部署到 K8s（持續流量）

K8s 上沒有本機目錄可掛腳本，所以把 `traffic.js` 等腳本 bake 進自帶映像，
由 GitHub Action（`.github/workflows/build-k6.yaml`）推到
`ghcr.io/<owner>/litebank/k6:latest`，再以常駐 Deployment 跑在 `lite-bank` namespace。

**重點：k6 透過 K8s service 直連 `http://api-gateway:8080`，不走外部 gateway/ingress（nip.io）。**

```bash
# 1) 確保映像已建好(改了 k6/** push 到 main 會自動觸發，或手動 workflow_dispatch)

# 2) 部署(預設 normal 情境、無限執行、50 VUs)
kubectl apply -f k6/k8s/k6-loadtest.yaml
#   或用 helper:
./deploy-k8s.sh                 # 預設 normal / infinite / 50 VUs
./deploy-k8s.sh hot 50 20m      # 覆蓋 MODE/VUS/DURATION

# 3) 觀察 / 控制
./deploy-k8s.sh logs            # 看流量 log
./deploy-k8s.sh stop            # 暫停(replicas=0)
./deploy-k8s.sh start           # 恢復
./deploy-k8s.sh delete          # 移除
```

「無限執行」：`DURATION=infinite`（或 `inf` / `0`）在 `config/options.js` 會換成 10 年，
配合 Deployment `restartPolicy: Always`，process 意外退出也會自動重啟 → 一直有流量。
要定時跑就把 `DURATION` 設成 `20m` 之類。

> 本地 docker 版仍用 `./run-k8s.sh`（走外部 nip.io ingress）；K8s 常駐版用 `./deploy-k8s.sh`（走 service）。

## 模擬的用戶行為

每個虛擬用戶會：
1. 隨機選擇 alice / bob / charlie 登入
2. 執行 3-6 個隨機操作
3. 操作之間有 1-3 秒的等待（模擬人類行為）

操作分佈：
- 30%: 查詢帳戶列表
- 25%: 查詢餘額
- 20%: 查詢交易歷史
- 10%: 查詢匯率
- 8%: 存款
- 7%: 轉帳

## 觀察結果

測試執行時，可以在 Grafana 觀察：
- **Tempo**: 查看分散式 traces
- **Prometheus**: 查看 metrics
- **Loki**: 查看 logs

Grafana URL: http://localhost:3000 (admin/admin)
