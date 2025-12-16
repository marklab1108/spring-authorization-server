# Spring Authorization Server - OAuth2 Client Credentials Flow

這是一個完整的 Spring Authorization Server 實作，支援 OAuth2 Client Credentials Flow 和 JWKS endpoint。

## 功能特性

- ✅ OAuth2 Client Credentials Flow 支援
- ✅ JWKS (JSON Web Key Set) Endpoint
- ✅ PostgreSQL 資料庫（生產環境就緒）
- ✅ 資料庫 Schema 和初始資料
- ✅ JWT Token 簽發
- ✅ Schema 隔離設計

## 技術棧

- Spring Boot 4.0.0
- Spring Security OAuth2 Authorization Server
- Spring Data JPA
- PostgreSQL 資料庫
- Java 21

## 專案結構

```
SpringAuthorizationServer/
├── source/                                               # 外部資源
│   ├── db/
│   │   ├── DDL.sql                                      # PostgreSQL DDL
│   │   ├── DML.sql                                      # PostgreSQL DML
│   │   └── README.md                                    # DB 設置指南
│   ├── postman-collection.json                          # Postman 測試
│   └── test-oauth.sh                                    # 測試腳本
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── DemoApplication.java
│   │   │       ├── config/
│   │   │       │   └── AuthorizationServerConfig.java  # OAuth2 配置
│   │   │       └── controller/
│   │   │           └── TestController.java             # 測試端點
│   │   └── resources/
│   │       └── application.yaml                        # 應用配置
│   └── test/
│       └── java/
│           └── com/example/demo/
│               └── DemoApplicationTests.java
├── pom.xml                                              # Maven 依賴
├── README.md                                            # 專案說明
```

## 快速啟動

### 前置要求
- Java 21
- PostgreSQL (運行於 127.0.0.1:5432)
- 資料庫 `authserver` 已創建

### Step 1: 設置資料庫

### Step 2: 啟動應用

```bash
# 編譯專案
./mvnw clean package

# 啟動應用
./mvnw spring-boot:run
```

應用將在 `http://localhost:9000` 啟動

### Step 3: 測試

```bash
# 使用自動化測試腳本
cd source
./test-oauth.sh
```

## 預設的 OAuth2 Clients

資料庫中已預設建立兩個測試用的 OAuth2 clients：

### Client 1: messaging-client
- **Client ID**: `messaging-client`
- **Client Secret**: `secret`
- **Grant Type**: `client_credentials`
- **Scopes**: `message.read`, `message.write`

### Client 2: api-client
- **Client ID**: `api-client`
- **Client Secret**: `api-secret`
- **Grant Type**: `client_credentials`
- **Scopes**: `api.read`, `api.write`, `api.delete`

## OAuth2 端點

### 1. Token Endpoint (取得 Access Token)
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u messaging-client:secret \
  -d "grant_type=client_credentials&scope=message.read message.write"
```

或使用 POST body 傳遞 client credentials：
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=messaging-client" \
  -d "client_secret=secret" \
  -d "scope=message.read message.write"
```

回應範例：
```json
{
  "access_token": "eyJraWQiOiI...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "message.read message.write"
}
```

### 2. JWKS Endpoint (取得公鑰)
```bash
curl http://localhost:9000/oauth2/jwks
```

回應範例：
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "kid": "...",
      "n": "..."
    }
  ]
}
```

### 3. Authorization Server Metadata
```bash
curl http://localhost:9000/.well-known/oauth-authorization-server
```

這個端點會返回所有可用的 OAuth2 端點資訊。

### 4. OpenID Configuration (如需要 OIDC)
```bash
curl http://localhost:9000/.well-known/openid-configuration
```

## 資料庫管理

### 使用 psql 命令行

```bash
# 連接到資料庫
psql -h 127.0.0.1 -p 5432 -U postgres -d authserver

# 查看所有表格
\dt poc_spring_authorization_server.*

# 查看客戶端資料
SELECT client_id, client_name, scopes 
FROM poc_spring_authorization_server.oauth2_registered_client;
```

### 使用 pgAdmin
可以使用 pgAdmin 圖形介面管理 PostgreSQL 資料庫：
- Host: 127.0.0.1
- Port: 5432
- Database: authserver
- Username: postgres
- Password: dj/4ej03

## 驗證 JWT Token

取得 token 後，可以使用 https://jwt.io 來解碼和驗證 token 內容。

Token 範例結構：
```json
{
  "header": {
    "alg": "RS256",
    "kid": "..."
  },
  "payload": {
    "sub": "messaging-client",
    "aud": "messaging-client",
    "nbf": 1234567890,
    "scope": ["message.read", "message.write"],
    "iss": "http://localhost:9000",
    "exp": 1234571490,
    "iat": 1234567890
  }
}
```

## 測試端點

專案包含以下測試端點：

### 1. 公開端點 (無需認證)
```bash
curl http://localhost:9000/api/public
```

### 2. 受保護端點 (需要 Token)
```bash
curl http://localhost:9000/api/protected \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. 訊息端點 (需要 Token 和 message.read scope)
```bash
curl http://localhost:9000/api/messages \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 使用 Access Token 呼叫 API

取得 token 後，可以在 API 請求中使用：
```bash
# 範例：呼叫受保護的端點
curl http://localhost:9000/api/protected \
  -H "Authorization: Bearer eyJraWQiOiI..."
```

回應範例：
```json
{
  "message": "This is a protected endpoint",
  "clientId": "messaging-client",
  "authorities": [...],
  "scopes": ["message.read", "message.write"],
  "timestamp": "1702384800000"
}
```

## 開發注意事項

1. **密碼加密**: 目前使用 `{noop}` prefix 儲存明文密碼，生產環境應該使用 bcrypt：
   ```java
   PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
   String encodedSecret = encoder.encode("secret");
   ```

2. **RSA Key Pair**: 目前在記憶體中動態生成，重啟後會改變。生產環境應該使用持久化的金鑰或從設定檔載入。

3. **Issuer URL**: 在 `AuthorizationServerConfig` 中設定為 `http://localhost:9000`，部署時需要修改為實際的域名。

4. **PostgreSQL 連線**: 
   - 確保 PostgreSQL 服務正在運行
   - 資料庫連線資訊在 `application.yaml` 中配置
   - Schema 使用 `poc_spring_authorization_server` 進行隔離

## 資料庫 Schema

詳細的資料庫結構請參考：
- `source/db/DDL.sql` - PostgreSQL 表格定義
- `source/db/DML.sql` - 預設的測試資料
- `source/db/README.md` - 完整的資料庫設置指南

主要資料表（位於 schema: `poc_spring_authorization_server`）：
- `oauth2_registered_client` - OAuth2 客戶端資訊
- `oauth2_authorization` - 授權資訊（包含 tokens）
- `oauth2_authorization_consent` - 授權同意資訊

## 測試流程

### 方法 1: 使用自動化測試腳本 (推薦)

執行提供的測試腳本：
```bash
./test-oauth.sh
```

這個腳本會自動測試所有主要功能：
- 獲取授權伺服器元數據
- 獲取 JWKS 公鑰
- 使用 Client Credentials Flow 獲取 Access Token
- 使用 Token 呼叫受保護的 API
- 測試無 Token 的存取（預期失敗）

### 方法 2: 使用 Postman

1. 匯入 `postman-collection.json` 到 Postman
2. 建立一個新的 Environment
3. 按照順序執行 collection 中的請求
4. Access Token 會自動儲存到環境變數中

### 方法 3: 手動測試

1. 啟動應用
2. 使用 curl 或 Postman 取得 access token
3. 驗證 token 格式和內容
4. 檢查 JWKS endpoint 是否正常運作
5. 使用 H2 console 檢查資料庫內容

## 故障排除

如遇到問題，可以：
1. 查看日誌輸出 (已啟用 Security 的 DEBUG 日誌)
2. 訪問 H2 控制台檢查資料
3. 確認 client credentials 是否正確
4. 驗證請求的 scope 是否存在

## 擴展功能

如需支援其他 OAuth2 flows，可以在 `data.sql` 中修改 client 的 `authorization_grant_types`：
- `authorization_code` - 授權碼模式
- `refresh_token` - Refresh Token
- `password` - 密碼模式 (不建議)
- `client_credentials` - 客戶端憑證模式 (已支援)
