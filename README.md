# Spring Authorization Server POC

這是一個 Spring Authorization Server 的 POC，支援：
- OAuth2 `client_credentials`
- OAuth2 `authorization_code`（整合外部認證 + 自訂授權條款頁 `/terms`）

## 技術棧
| 技術 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.3.6 |
| Spring Authorization Server | 由 Spring Boot 管理 |
| PostgreSQL | 15+ |
| Thymeleaf | 由 Spring Boot 管理 |

## 專案結構
```
spring-authorization-server/
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java          # 應用程式入口
│   ├── config/
│   │   └── AuthorizationServerConfig.java  # OAuth2 授權伺服器設定
│   ├── controller/
│   │   ├── ExternalAuthCallbackController.java  # 外部認證回調處理
│   │   ├── ExternalLoginController.java         # 外部登入入口
│   │   └── TermsController.java                 # 授權條款頁
│   ├── dto/
│   │   ├── ExternalAuthCallbackDto.java   # 回調資料 DTO
│   │   ├── ExternalUserInfoRequest.java   # 外部 API 請求
│   │   └── ExternalUserInfoResponse.java  # 外部 API 回應
│   ├── entity/
│   │   └── ConsentHistory.java            # Consent 歷史記錄 Entity
│   ├── exception/
│   │   ├── AuthException.java             # 自訂例外
│   │   └── GlobalExceptionHandler.java    # 全域例外處理
│   ├── repository/
│   │   └── ConsentHistoryRepository.java  # Consent 歷史記錄 Repository
│   └── service/
│       ├── AuditableConsentService.java   # 每次授權都要同意的 Consent Service
│       └── ExternalAuthService.java       # 外部認證服務
├── src/main/resources/
│   ├── application.yaml                   # 應用程式設定
│   └── templates/                         # Thymeleaf 模板
│       ├── auth-home-page.html
│       ├── error.html
│       └── terms.html
├── source/
│   ├── db/
│   │   ├── DDL.sql                        # 資料表建立腳本
│   │   └── DML.sql                        # 測試資料腳本
│   ├── mock-external-server/              # Mock 外部認證系統
│   ├── test-ap/                           # Python 測試工具
│   ├── test-authorization-code-flow.sh   # Authorization Code 測試腳本
│   ├── test-oauth.sh                      # Client Credentials 測試腳本
│   └── postman-collection.json            # Postman 集合
└── pom.xml
```

## 需求與前置
- Java 21
- PostgreSQL 15+（本機測試用）
- Maven 3.9+

## 資料庫設定
### 1) 建立 Schema 與資料表
- DDL: source/db/DDL.sql
- DML: source/db/DML.sql

### 2) 設定連線

編輯 `src/main/resources/application.yaml`，調整資料庫連線設定：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/authserver
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:your-password}
```

## 啟動方式

### 1) 啟動 Authorization Server

```bash
./mvnw clean package
./mvnw spring-boot:run
```

### 2) 啟動 Mock External Server

```bash
cd source/mock-external-server
./mvnw spring-boot:run
```

## 主要端點

### Authorization Server（9000）

| 端點 | 說明 |
|------|------|
| `GET /oauth2/authorize` | 授權端點 |
| `POST /oauth2/token` | Token 端點 |
| `GET /oauth2/jwks` | JWKS 公鑰端點 |
| `GET /.well-known/oauth-authorization-server` | 授權伺服器 metadata |
| `GET /.well-known/openid-configuration` | OIDC Discovery |

自訂頁面 / 流程端點：

| 端點 | 說明 |
|------|------|
| `GET /external-login` | 中轉頁（導向外部系統認證） |
| `GET /oauth2/callback?data=...` | 外部回調入口（Base64 JSON） |
| `GET /terms` | 授權條款頁（SAS consentPage） |

### Mock External Server（8888）

| 端點 | 說明 |
|------|------|
| `GET /login?session=...&callback_url=...` | 登入頁 |
| `POST /login` | 提交登入（導回 callback_url） |
| `GET /test-login?session=...&callback_url=...&customer_id=...` | 免表單快速登入（測試用） |
| `POST /api/userinfo` | 外部使用者資訊 API |
| `GET /health` | 健康檢查端點 |

## OAuth2 Flows

### Client Credentials Flow

適用於機器對機器（M2M）的場景，不需要使用者介入。

```bash
# 取得 Access Token
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u messaging-client:secret \
  -d "grant_type=client_credentials&scope=message.read message.write"
```

或使用測試腳本：
```bash
cd source
./test-oauth.sh
```

### Authorization Code Flow（外部認證 + 條款頁）

整體流程（重點：授權碼由 Spring Authorization Server 簽發，不手工產生）：

1. Client 發起授權請求到 `GET /oauth2/authorize?...`
2. 若使用者尚未登入，Spring Security 會導向 `GET /external-login`（從 SavedRequest 取回原始授權請求資訊）
3. 使用者在 `/external-login` 點擊按鈕 → 導向 Mock External Server `/login`
4. 外部系統認證成功後導回 `GET /oauth2/callback?data=...`
   - `data` 為 Base64(JSON)：`{statusCode,statusDesc,session,token}`
   - Authorization Server 驗證 `statusCode=="0000"`、session 一致性後，再呼叫外部 API `/api/userinfo` 取得 `customerId`
   - 成功後寫入 `SecurityContext`（等同完成登入）
5. 使用者被導回原始的 `/oauth2/authorize`，Spring Authorization Server 進入 consentPage `/terms`
6. 使用者同意條款後，表單 POST 回 `/oauth2/authorize`，由 Spring Authorization Server 簽發 `authorization_code` 並 redirect 回 client `redirect_uri`
7. Client 使用 `authorization_code` 交換 token：`POST /oauth2/token`

```
┌──────────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │     │ Auth Server │     │ Mock Server │     │    User      │
└────┬─────┘     └──────┬──────┘     └──────┬──────┘     └──────┬───────┘
     │                  │                   │                   │
     │ 1. /oauth2/authorize                 │                   │
     │─────────────────>│                   │                   │
     │                  │                   │                   │
     │                  │ 2. Redirect /external-login           │
     │                  │───────────────────────────────────────>
     │                  │                   │                   │
     │                  │                   │  3. Click login   │
     │                  │                   │<──────────────────│
     │                  │                   │                   │
     │                  │ 4. Redirect /login (Mock)             │
     │                  │<─────────────────>│<─────────────────>│
     │                  │                   │                   │
     │                  │ 5. /oauth2/callback?data=...          │
     │                  │<──────────────────│                   │
     │                  │                   │                   │
     │                  │ 6. Call /api/userinfo                 │
     │                  │──────────────────>│                   │
     │                  │<──────────────────│                   │
     │                  │                   │                   │
     │                  │ 7. Redirect /terms                    │
     │                  │───────────────────────────────────────>
     │                  │                   │                   │
     │                  │                   │  8. Agree terms   │
     │                  │<──────────────────────────────────────│
     │                  │                   │                   │
     │ 9. Redirect with code                │                   │
     │<─────────────────│                   │                   │
     │                  │                   │                   │
     │ 10. POST /oauth2/token               │                   │
     │─────────────────>│                   │                   │
     │<─────────────────│                   │                   │
     │  (access_token)  │                   │                   │
```

## 測試

### 自動化腳本（推薦）

```bash
cd source
./test-authorization-code-flow.sh
```

### 手動測試

1. 打開：
   `http://localhost:9000/oauth2/authorize?response_type=code&client_id=client-web&redirect_uri=http://localhost:8080/callback&scope=profile%20email&state=xyz123`
2. 依序完成 `/external-login` → 外部登入 → `/terms` 同意條款
3. 以授權碼交換 token：

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u client-web:web-secret \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:8080/callback"
```

### Python 測試工具

```bash
cd source/test-ap
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python test_oauth.py
```

### Postman Collection

匯入 `source/postman-collection.json` 至 Postman 進行 API 測試。

## 設定

本專案使用 `external-auth.*` 設定外部系統位置（定義在 `application.yaml`）：

```yaml
external-auth:
  server-url: http://localhost:8888
  login-endpoint: /login
  api-endpoint: /api/userinfo
  platform-id: authserver
  connect-timeout-ms: 5000
  read-timeout-ms: 5000

authorization-server:
  issuer: http://localhost:9000
```

對應環境變數（Spring Boot relaxed binding）：

| 環境變數 | 說明 |
|----------|------|
| `EXTERNAL_AUTH_SERVER_URL` | 外部認證系統 URL |
| `EXTERNAL_AUTH_LOGIN_ENDPOINT` | 登入端點路徑 |
| `EXTERNAL_AUTH_API_ENDPOINT` | 使用者資訊 API 路徑 |
| `EXTERNAL_AUTH_PLATFORM_ID` | 平台識別碼 |
| `EXTERNAL_AUTH_CONNECT_TIMEOUT_MS` | 連線逾時（毫秒） |
| `EXTERNAL_AUTH_READ_TIMEOUT_MS` | 讀取逾時（毫秒） |
| `AUTHORIZATION_SERVER_ISSUER` | OAuth2 Issuer URL |

## 測試用 OAuth2 Clients（DB 預置）

僅供本機/POC 測試：

| Client ID | Secret | Grant Types | 說明 |
|-----------|--------|-------------|------|
| `messaging-client` | `secret` | `client_credentials` | 訊息服務 |
| `api-client` | `api-secret` | `client_credentials` | API 服務 |
| `client-web` | `web-secret` | `authorization_code`, `refresh_token` | Web 應用程式 |

> ⚠️ **注意**：測試用密碼使用 `{noop}` 前綴（明文），生產環境請使用 BCrypt 編碼：
> ```java
> String encoded = new BCryptPasswordEncoder().encode("your-secret");
> // 結果如：{bcrypt}$2a$10$...
> ```

## 授權同意機制（Consent）

### 設計特點

本專案實作了**每次授權都要同意條款**的機制：

- 使用 `authorization_code` flow 的 client，每次授權都會顯示條款頁
- 每次同意都會記錄到 `oauth2_consent_history` 表，供審計使用
- 不使用 Spring SAS 原生的 consent 記憶機制

### Consent 歷史記錄

| 欄位 | 說明 |
|------|------|
| `id` | 自動遞增主鍵 |
| `registered_client_id` | OAuth2 Client 內部 ID |
| `principal_name` | 用戶識別碼（customerId） |
| `scopes` | 授權的 scopes |
| `consent_time` | 同意時間 |

### 定期清理

建議定期清理超過 1 年的歷史記錄。可使用以下方式：

**方式 1：PostgreSQL pg_cron**
```sql
-- 每天凌晨 3 點執行清理
SELECT cron.schedule('0 3 * * *', $$
    DELETE FROM poc_spring_authorization_server.oauth2_consent_history 
    WHERE consent_time < NOW() - INTERVAL '1 year'
$$);
```

**方式 2：Spring @Scheduled**

在應用程式中加入定時任務：
```java
@Scheduled(cron = "0 0 3 * * ?")
@Transactional
public void cleanupOldConsentHistory() {
    Instant cutoff = Instant.now().minus(365, ChronoUnit.DAYS);
    consentHistoryRepository.deleteByConsentTimeBefore(cutoff);
}
```

**方式 3：外部 Cron Job**
```bash
# crontab -e
0 3 * * * psql -U postgres -d authserver -c "DELETE FROM poc_spring_authorization_server.oauth2_consent_history WHERE consent_time < NOW() - INTERVAL '1 year';"
```

## 已知限制與未來規劃

### 目前限制

| 項目 | 說明 | 規劃 |
|------|------|------|
| RSA 金鑰 | 每次啟動重新生成，重啟後已發出的 JWT 失效 | Phase 2: 持久化至 KeyStore |
| PKCE | 目前未啟用 | Phase 2: 啟用 PKCE 支援 |
| Token 撤銷 | 未實作 Token Revocation | Phase 3 |
| 多實例部署 | 需共享金鑰與 Session | Phase 3: Redis Session Store |

### 安全性注意事項

- 本 POC 的資料庫密碼、Client Secret 等敏感資訊請勿直接 commit
- 生產環境請使用環境變數或 Secret Manager

## 問題排除

### 常見問題

**Q: 啟動時出現 `Failed to configure a DataSource`**
A: 確認 PostgreSQL 是否啟動，以及 `application.yaml` 中的連線設定是否正確。

**Q: 授權流程中出現 `找不到授權請求`**
A: Session 可能已過期，請重新發起授權流程。

**Q: 外部認證後無法回到條款頁**
A: 確認 Mock External Server 是否正常運行（port 8888）。

**Q: Token 驗證失敗**
A: 若服務重啟過，RSA 金鑰會重新生成，舊 Token 會失效。

## License

MIT License
