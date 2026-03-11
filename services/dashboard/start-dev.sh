#!/bin/bash

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}正在啟動開發 server...${NC}"

# 獲取當前腳本所在目錄
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

# 檢查是否有正在運行的 Vite 開發 server
echo -e "${YELLOW}檢查是否有舊的開發 server 正在運行...${NC}"

# 尋找運行在 3001 端口的進程 (vite.config.js 設定的端口)
VITE_PID=$(lsof -ti:3001)

if [ ! -z "$VITE_PID" ]; then
    echo -e "${YELLOW}發現正在運行的開發 server (PID: $VITE_PID)${NC}"
    echo -e "${YELLOW}正在停止舊的 server...${NC}"
    kill -9 $VITE_PID
    sleep 1
    echo -e "${GREEN}✓ 已停止舊的 server${NC}"
else
    echo -e "${GREEN}✓ 沒有發現正在運行的 server${NC}"
fi

# 檢查是否有 npm run dev 的進程 (備用檢查)
NPM_DEV_PID=$(pgrep -f "vite" | head -n 1)
if [ ! -z "$NPM_DEV_PID" ]; then
    echo -e "${YELLOW}發現殘留的 vite 進程 (PID: $NPM_DEV_PID)${NC}"
    kill -9 $NPM_DEV_PID
    sleep 1
    echo -e "${GREEN}✓ 已清理殘留進程${NC}"
fi

echo ""
echo -e "${GREEN}正在啟動新的開發 server...${NC}"
echo ""

# 啟動開發 server
npm run dev
