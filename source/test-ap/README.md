# OAuth2 Client Credentials Flow Python 測試工具

這是一個用於測試 Spring Authorization Server 的 Python 工具，實作了完整的 OAuth2 Client Credentials Flow 和 JWT Token 驗證。

## 功能特性

✅ **OAuth2 Client Credentials Flow**
   - 使用 messaging-client 自動取得 access token
   - 支援 HTTP Basic Authentication

✅ **JWKS 公鑰驗證**
   - 自動從 JWKS 端點取得公鑰
   - 完整的 JWT 簽名驗證
   - 驗證 token 的 issuer 和 audience

✅ **虛擬環境管理**
   - 自動建立和管理 Python 虛擬環境
   - 自動安裝所需依賴
   - 環境隔離，不影響系統 Python

## 前置要求

- Python 3.8 或更高版本
- Spring Authorization Server 運行於 http://localhost:9000
- messaging-client 已在資料庫中設定（預設配置）

## 快速開始

### 方法 1: 使用啟動腳本（推薦）

最簡單的方式，一鍵啟動：

```bash
cd source/test-ap
chmod +x run.sh
./run.sh
```

腳本會自動：
1. 檢查 Python 3 是否安裝
2. 建立虛擬環境（如果不存在）
3. 安裝所需依賴
4. 檢查 Authorization Server 狀態
5. 執行測試
6. 自動清理和停用虛擬環境

### 方法 2: 手動執行

如果你想手動控制每個步驟：

```bash
cd source/test-ap

# 建立虛擬環境
python3 -m venv venv

# 啟動虛擬環境
source venv/bin/activate

# 安裝依賴
pip install -r requirements.txt

# 執行測試
python test_oauth.py

# 完成後停用虛擬環境
deactivate
```

## 測試流程

測試腳本會按照以下順序執行：

### 步驟 1: 取得 Access Token
- 向 `/oauth2/token` 端點發送請求
- 使用 Client Credentials Flow
- Client ID: `messaging-client`
- Client Secret: `secret`
- Scopes: `message.read message.write`

### 步驟 2: 取得 JWKS 公鑰
- 從 `/oauth2/jwks` 端點取得公鑰集合
- 顯示公鑰資訊（Key Type, Key ID, Algorithm）

### 步驟 3: 驗證 Access Token
- 使用 JWKS 公鑰驗證 JWT 簽名
- 驗證 issuer 和 audience
- 解碼並顯示 token 內容
- 顯示 token 的簽發時間和到期時間

## 輸出範例

```
================================================================================
開始 OAuth2 Client Credentials Flow 測試
================================================================================

================================================================================
步驟 1: 取得 Access Token
================================================================================
Token Endpoint: http://localhost:9000/oauth2/token
Client ID: messaging-client
Client Secret: ******
Scopes: message.read message.write

✅ 成功取得 Access Token
Token Type: Bearer
Expires In: 3600 秒
Scope: message.read message.write

Access Token (前 50 個字元): eyJraWQiOiI5ZTk0ZGM4Yy0zYjk1LTRhYmEtOGE5Yy04ZTk0...

================================================================================
步驟 2: 取得 JWKS 公鑰
================================================================================
JWKS Endpoint: http://localhost:9000/oauth2/jwks

✅ 成功取得 JWKS
公鑰數量: 1

公鑰 #1:
  Key Type: RSA
  Key ID: 9e94dc8c-3b95-4aba-8a9c-8e94dc8c3b95
  Algorithm: N/A
  Use: N/A

================================================================================
步驟 3: 驗證 Access Token
================================================================================
✅ Token 驗證成功！

Token Header:
{
  "alg": "RS256",
  "kid": "9e94dc8c-3b95-4aba-8a9c-8e94dc8c3b95"
}

Token Payload:
{
  "sub": "messaging-client",
  "aud": "messaging-client",
  "nbf": 1702384800,
  "scope": ["message.read", "message.write"],
  "iss": "http://localhost:9000",
  "exp": 1702388400,
  "iat": 1702384800
}

到期時間: 2025-12-12 14:00:00
簽發時間: 2025-12-12 13:00:00

================================================================================
測試完成總結
================================================================================
✅ 1. 成功使用 Client Credentials Flow 取得 Access Token
✅ 2. 成功從 JWKS 端點取得公鑰
✅ 3. 成功驗證 Access Token 的簽名和內容

所有測試通過！🎉
================================================================================
```

## 檔案說明

### test_oauth.py
主要的測試腳本，包含：
- `OAuth2Tester` 類別：封裝所有測試邏輯
- `get_access_token()`: 取得 access token
- `get_jwks()`: 取得 JWKS 公鑰
- `verify_token()`: 驗證 JWT token

### requirements.txt
Python 依賴清單：
- `requests`: HTTP 請求
- `PyJWT[crypto]`: JWT 解碼和驗證
- `cryptography`: 加密和公鑰處理

### run.sh
啟動腳本，自動處理虛擬環境和依賴安裝

## 自訂配置

如需測試其他 client，可以修改 `test_oauth.py` 中的參數：

```python
tester = OAuth2Tester(
    auth_server_url="http://localhost:9000",  # Authorization Server URL
    client_id="api-client",                    # 改為 api-client
    client_secret="api-secret",                # 對應的 secret
    scopes="api.read api.write"                # 對應的 scopes
)
```

## 虛擬環境管理

### 為什麼使用虛擬環境？
- 隔離專案依賴，避免與系統 Python 衝突
- 方便管理不同專案的套件版本
- 符合 Python 最佳實踐

### 虛擬環境命令
```bash
# 啟動虛擬環境
source venv/bin/activate

# 停用虛擬環境
deactivate

# 重建虛擬環境（如果遇到問題）
rm -rf venv
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## 故障排除

### 錯誤: 無法連接到 Authorization Server
**解決方案**: 確認 Spring Authorization Server 已啟動並運行在 http://localhost:9000

```bash
# 檢查 server 狀態
curl http://localhost:9000/.well-known/oauth-authorization-server
```

### 錯誤: Token 驗證失敗
**可能原因**:
1. Token 已過期
2. Issuer URL 不匹配
3. JWKS 公鑰變更（server 重啟）

**解決方案**: 重新執行測試腳本取得新的 token

### 錯誤: 找不到 Python 3
**解決方案**: 安裝 Python 3

```bash
# macOS
brew install python3

# Ubuntu/Debian
sudo apt-get install python3 python3-venv
```

## 與其他測試工具比較

| 工具 | 優點 | 缺點 |
|------|------|------|
| **Python 腳本** | 完整驗證流程、可程式化、適合自動化 | 需要安裝 Python |
| **test-oauth.sh** | 簡單快速、無需額外依賴 | 無法驗證 JWT 簽名 |
| **Postman** | 視覺化介面、易於除錯 | 需要手動操作 |

## 進階使用

### 整合到 CI/CD
腳本的退出碼（exit code）反映測試結果：
- `0`: 所有測試通過
- `1`: 測試失敗

可以整合到自動化測試流程：

```bash
#!/bin/bash
cd source/test-ap
./run.sh
if [ $? -eq 0 ]; then
    echo "OAuth2 tests passed"
else
    echo "OAuth2 tests failed"
    exit 1
fi
```

### 作為 Python 模組使用
可以在其他 Python 程式中匯入使用：

```python
from test_oauth import OAuth2Tester

tester = OAuth2Tester()
token_response = tester.get_access_token()
decoded = tester.verify_token()

# 使用 token 進行其他操作
print(f"Token: {tester.access_token}")
```

## 相關文件

- [Spring Authorization Server README](../../README.md)
- [Database Setup](../db/README.md)
- [Postman Collection](../postman-collection.json)

## 授權

本工具為專案的一部分，與主專案使用相同的授權。

