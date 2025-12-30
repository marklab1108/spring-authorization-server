#!/bin/bash

# OAuth2 測試腳本啟動器
# 自動建立虛擬環境並執行測試

set -e  # 遇到錯誤時停止執行

# 顏色定義
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 取得腳本所在目錄
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

VENV_DIR="venv"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}OAuth2 測試腳本啟動器${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 檢查 Python 3 是否安裝
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}❌ 錯誤: 未找到 Python 3${NC}"
    echo "請先安裝 Python 3"
    exit 1
fi

PYTHON_VERSION=$(python3 --version)
echo -e "${GREEN}✓${NC} Python 版本: $PYTHON_VERSION"
echo ""

# 檢查虛擬環境是否存在
if [ ! -d "$VENV_DIR" ]; then
    echo -e "${YELLOW}虛擬環境不存在，正在建立...${NC}"
    python3 -m venv "$VENV_DIR"
    echo -e "${GREEN}✓${NC} 虛擬環境建立完成"
    echo ""
fi

# 啟動虛擬環境
echo -e "${BLUE}正在啟動虛擬環境...${NC}"
source "$VENV_DIR/bin/activate"
echo -e "${GREEN}✓${NC} 虛擬環境已啟動"
echo ""

# 升級 pip
echo -e "${BLUE}正在升級 pip...${NC}"
pip3 install --upgrade pip -q
echo -e "${GREEN}✓${NC} pip 已升級至最新版本"
echo ""

# 安裝依賴
echo -e "${BLUE}正在檢查並安裝依賴...${NC}"
pip3 install -r requirements.txt -q
echo -e "${GREEN}✓${NC} 所有依賴已安裝"
echo ""

# 顯示已安裝的套件
echo -e "${BLUE}已安裝的主要套件:${NC}"
pip3 list | grep -E "requests|PyJWT|cryptography"
echo ""

# 檢查 Authorization Server 是否運行
echo -e "${BLUE}檢查 Authorization Server 狀態...${NC}"
if curl -s -f http://localhost:9000/.well-known/oauth-authorization-server > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Authorization Server 正在運行"
else
    echo -e "${YELLOW}⚠${NC}  警告: 無法連接到 Authorization Server (http://localhost:9000)"
    echo "請確認 Spring Authorization Server 已啟動"
    echo ""
    read -p "是否繼續執行測試? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
echo ""

# 執行測試腳本
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}執行測試腳本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

python3 test_oauth.py

# 測試結束
echo ""
echo -e "${GREEN}測試執行完成！${NC}"
echo ""
echo -e "${YELLOW}提示: 虛擬環境已自動停用${NC}"
echo "如需手動進入虛擬環境，執行: source venv/bin/activate"
echo "如需停用虛擬環境，執行: deactivate"
echo ""

# 停用虛擬環境
deactivate

