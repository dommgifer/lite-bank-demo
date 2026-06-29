#!/bin/bash
# ============================================
# Lite Bank k6 — 部署持續流量產生器到 K8s
#
# 對應的本地版是 run-k8s.sh(docker run)。這支是 K8s 版:把 k6 當常駐
# Deployment 跑在 lite-bank namespace,透過 service 直連 api-gateway:8080,
# 不走外部 gateway api。預設 normal + 無限執行(一直有流量)。
#
# 用法:
#   ./deploy-k8s.sh                 # 套用 manifest(預設 normal / infinite / 50 VUs)
#   ./deploy-k8s.sh normal 80       # 覆蓋 MODE/VUS
#   ./deploy-k8s.sh hot 50 20m      # 覆蓋 MODE/VUS/DURATION(定時跑)
#
# 其他:
#   ./deploy-k8s.sh logs            # 看流量 log
#   ./deploy-k8s.sh stop            # 暫停(replicas=0)
#   ./deploy-k8s.sh start           # 恢復(replicas=1)
#   ./deploy-k8s.sh delete          # 移除 Deployment
#
# 映像由 .github/workflows/build-k6.yaml 建構推到:
#   ghcr.io/<owner>/litebank/k6:latest
# ============================================
set -euo pipefail

NS=lite-bank
DEPLOY=k6-loadtest
MANIFEST="$(dirname "$0")/k8s/k6-loadtest.yaml"
KUBECTL=${KUBECTL:-kubectl}

case "${1:-apply}" in
  logs)   exec $KUBECTL -n "$NS" logs -f deploy/"$DEPLOY" ;;
  stop)   $KUBECTL -n "$NS" scale deploy/"$DEPLOY" --replicas=0; echo "已暫停 $DEPLOY"; exit 0 ;;
  start)  $KUBECTL -n "$NS" scale deploy/"$DEPLOY" --replicas=1; echo "已恢復 $DEPLOY"; exit 0 ;;
  delete) $KUBECTL -n "$NS" delete -f "$MANIFEST"; exit 0 ;;
esac

# ---- apply 模式:可選覆蓋 MODE / VUS / DURATION ----
MODE="${1:-normal}"
VUS="${2:-}"
DURATION="${3:-}"

if [ "$MODE" != "normal" ] && [ "$MODE" != "hot" ]; then
  echo "錯誤:情境必須是 normal 或 hot(收到 '$MODE')" >&2
  echo "用法: ./deploy-k8s.sh [normal|hot] [VUS] [DURATION]" >&2
  exit 1
fi

echo "套用 manifest: $MANIFEST"
$KUBECTL apply -f "$MANIFEST"

# 套用後依參數覆蓋 env(會觸發滾動重啟)
ENV_ARGS=("MODE=$MODE")
[ -n "$VUS" ] && ENV_ARGS+=("VUS=$VUS")
[ -n "$DURATION" ] && ENV_ARGS+=("DURATION=$DURATION")

echo "設定 env: ${ENV_ARGS[*]}"
$KUBECTL -n "$NS" set env deploy/"$DEPLOY" "${ENV_ARGS[@]}"

echo "======================================"
echo "k6 已部署到 K8s (namespace: $NS)"
echo "  Target:   http://api-gateway:8080 (K8s service)"
echo "  Mode:     $MODE"
echo "  VUs:      ${VUS:-50 (manifest 預設)}"
echo "  Duration: ${DURATION:-infinite (manifest 預設)}"
echo "  看 log:   ./deploy-k8s.sh logs"
echo "  暫停:     ./deploy-k8s.sh stop"
echo "======================================"
