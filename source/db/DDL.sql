-- Spring Authorization Server Schema for PostgreSQL
-- Based on official Spring Authorization Server schema
-- Schema: poc_spring_authorization_server

-- OAuth2 Registered Client table
CREATE TABLE IF NOT EXISTS poc_spring_authorization_server.oauth2_registered_client (
    id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth2_registered_client_client_id 
    ON poc_spring_authorization_server.oauth2_registered_client (client_id);

-- OAuth2 Authorization table
CREATE TABLE IF NOT EXISTS poc_spring_authorization_server.oauth2_authorization (
    id VARCHAR(100) NOT NULL,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000),
    attributes TEXT,
    state VARCHAR(500),
    authorization_code_value TEXT,
    authorization_code_issued_at TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata TEXT,
    access_token_value TEXT,
    access_token_issued_at TIMESTAMP,
    access_token_expires_at TIMESTAMP,
    access_token_metadata TEXT,
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(1000),
    oidc_id_token_value TEXT,
    oidc_id_token_issued_at TIMESTAMP,
    oidc_id_token_expires_at TIMESTAMP,
    oidc_id_token_metadata TEXT,
    refresh_token_value TEXT,
    refresh_token_issued_at TIMESTAMP,
    refresh_token_expires_at TIMESTAMP,
    refresh_token_metadata TEXT,
    user_code_value TEXT,
    user_code_issued_at TIMESTAMP,
    user_code_expires_at TIMESTAMP,
    user_code_metadata TEXT,
    device_code_value TEXT,
    device_code_issued_at TIMESTAMP,
    device_code_expires_at TIMESTAMP,
    device_code_metadata TEXT,
    PRIMARY KEY (id)
);

-- OAuth2 Authorization Consent table (Spring SAS 原生表，本專案不使用)
-- 保留此表以相容 Spring SAS，但實際 consent 記錄存於 oauth2_consent_history
CREATE TABLE IF NOT EXISTS poc_spring_authorization_server.oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- ============================================================================
-- OAuth2 Consent History table (自訂：每次授權同意的軌跡記錄)
-- ============================================================================
-- 用途：記錄每次用戶同意授權的歷史，用於審計與合規
-- 特性：
--   1. 每次同意都會新增一筆記錄（不會覆蓋）
--   2. 確保每次授權都要重新同意條款
--   3. 建議定期清理超過 1 年的記錄
-- ============================================================================
CREATE TABLE IF NOT EXISTS poc_spring_authorization_server.oauth2_consent_history (
    id                    BIGSERIAL PRIMARY KEY,
    registered_client_id  VARCHAR(100) NOT NULL,
    principal_name        VARCHAR(200) NOT NULL,
    scopes                VARCHAR(1000) NOT NULL,
    consent_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引：加速查詢
CREATE INDEX IF NOT EXISTS idx_consent_history_principal 
    ON poc_spring_authorization_server.oauth2_consent_history (principal_name);

CREATE INDEX IF NOT EXISTS idx_consent_history_client 
    ON poc_spring_authorization_server.oauth2_consent_history (registered_client_id);

CREATE INDEX IF NOT EXISTS idx_consent_history_time 
    ON poc_spring_authorization_server.oauth2_consent_history (consent_time);

-- ============================================================================
-- 定期清理範例（PostgreSQL pg_cron 或外部排程執行）
-- ============================================================================
-- DELETE FROM poc_spring_authorization_server.oauth2_consent_history 
-- WHERE consent_time < NOW() - INTERVAL '1 year';
