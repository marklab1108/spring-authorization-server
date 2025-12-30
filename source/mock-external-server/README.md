# Mock External Authentication Server

這是一個用來配合本 POC 的「外部認證系統」Mock Server（Spring Boot）。

預設啟動在 `http://localhost:8888`。

## 端點

### 1) 外部登入頁
`GET /login?session=...&callback_url=...`

顯示登入表單。

### 2) 提交登入
`POST /login`

表單欄位：
- `session`
- `callback_url`
- `customer_id`

登入成功後，會 redirect 到：
`{callback_url}?data=...`

其中 `data` 為 Base64(JSON)：
```json
{
  "statusCode": "0000",
  "statusDesc": "OK",
  "session": "uuid_client-web",
  "token": "external-token"
}
```

### 3) 快速登入（測試用）
`GET /test-login?session=...&callback_url=...&customer_id=...`

免表單直接成功登入，並回傳同樣的 `data`。

### 4) 外部使用者資訊 API
`POST /api/userinfo`

Request：
```json
{
  "platformId": "authserver",
  "token": "external-token"
}
```

Response：
```json
{
  "statusCode": "0000",
  "statusDesc": "OK",
  "customerId": "A123456789"
}
```

### 5) Health Check
`GET /health`

## 啟動

```bash
./mvnw spring-boot:run
```

