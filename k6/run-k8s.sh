#!/bin/bash
# ============================================
# Lite Bank k6 流量模擬 — K8s 環境專用
#
# 用法:
#   ./run-k8s.sh                  # 不帶參數 → 互動選擇情境
#   ./run-k8s.sh normal           # 一般情境(分散打,看真實 SLO)
#   ./run-k8s.sh hot              # 熱點情境(集中壓單一帳戶的行鎖)
#   ./run-k8s.sh normal 50 20m    # 指定 VUS / DURATION
#
# 背景長跑(不佔 terminal):前面加 DETACH=1
#   DETACH=1 ./run-k8s.sh hot 50 120m
#   docker logs -f litebank-k6    # 看進度
#   docker stop litebank-k6       # 停止
#
# 可用環境變數覆蓋:BASE_URL / USER_POOL / HOT_USER
# ============================================
set -euo pipefail

# K8s 進入點與用戶池(對應 db/migration/V10 種的 loadtest_0001..0200)
BASE_URL=${BASE_URL:-http://litebank.192.168.1.70.nip.io}
USER_POOL=${USER_POOL:-200}
HOT_USER=${HOT_USER:-loadtest_0001}

# ---- 選情境:第一個參數,沒帶就互動選 ----
MODE="${1:-}"
if [ -z "$MODE" ]; then
  echo "選擇流量情境:"
  echo "  1) normal — 一般情境(每個 VU 綁不同用戶、分散打,看真實 SLO)"
  echo "  2) hot    — 熱點情境(所有 VU 集中壓同一帳戶,壓鎖爭用)"
  read -rp "請輸入 1 或 2 [1]: " choice
  case "${choice:-1}" in
    1) MODE=normal ;;
    2) MODE=hot ;;
    *) echo "無效選擇:'$choice'" >&2; exit 1 ;;
  esac
fi

if [ "$MODE" != "normal" ] && [ "$MODE" != "hot" ]; then
  echo "錯誤:情境必須是 normal 或 hot(收到 '$MODE')" >&2
  echo "用法: ./run-k8s.sh [normal|hot] [VUS] [DURATION]" >&2
  exit 1
fi

VUS=${2:-50}
DURATION=${3:-20m}

# normal 模式 VUS 不該超過用戶池,否則退回假熱點(碰撞)
if [ "$MODE" = "normal" ] && [[ "$VUS" =~ ^[0-9]+$ ]] && [ "$VUS" -gt "$USER_POOL" ]; then
  echo "⚠️  VUS($VUS) > USER_POOL($USER_POOL):部分 VU 會共用同一用戶、出現碰撞"
fi

echo "======================================"
echo "Lite Bank k6 — K8s"
echo "Mode:      $MODE"
echo "VUs:       $VUS"
echo "Duration:  $DURATION"
echo "Target:    $BASE_URL"
echo "User pool: $USER_POOL"
[ "$MODE" = "hot" ] && echo "Hot user:  $HOT_USER"
echo "======================================"

# 前景(--rm -i,Ctrl+C 停) / 背景(DETACH=1 → --rm -d)
DOCKER_FLAGS="--rm -i"
[ "${DETACH:-0}" = "1" ] && DOCKER_FLAGS="--rm -d"

docker run $DOCKER_FLAGS \
  --name litebank-k6 \
  -e BASE_URL="$BASE_URL" \
  -e VUS="$VUS" \
  -e DURATION="$DURATION" \
  -e MODE="$MODE" \
  -e USER_POOL="$USER_POOL" \
  -e HOT_USER="$HOT_USER" \
  -v "$(dirname "$0"):/scripts" \
  grafana/k6 run /scripts/traffic.js

if [ "${DETACH:-0}" = "1" ]; then
  echo "已背景啟動 (container: litebank-k6)"
  echo "  看 log: docker logs -f litebank-k6"
  echo "  停止:   docker stop litebank-k6"
fi
