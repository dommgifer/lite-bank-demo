#!/bin/bash

# 預設值
VUS=${1:-10}
DURATION=${2:-5m}
BASE_URL=${3:-http://host.docker.internal:9000}

echo "======================================"
echo "Lite Bank k6 流量模擬"
echo "VUs: $VUS"
echo "Duration: $DURATION"
echo "Target: $BASE_URL"
echo "======================================"

docker run --rm -i \
  -e BASE_URL="$BASE_URL" \
  -e VUS="$VUS" \
  -e DURATION="$DURATION" \
  -v "$(dirname "$0"):/scripts" \
  grafana/k6 run /scripts/traffic.js
