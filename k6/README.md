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
