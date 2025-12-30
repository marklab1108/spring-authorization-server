package com.example.demo.constant;

/**
 * HTTP Session 屬性名稱常數
 * 
 * 集中管理所有 Session 相關的 key，避免魔術字串分散各處。
 */
public final class SessionKeys {
    
    private SessionKeys() {
        // 禁止實例化
    }
    
    // ========== OAuth2 授權流程相關 ==========
    
    /** OAuth2 Client ID */
    public static final String CLIENT_ID = "oauth2_client_id";
    
    /** OAuth2 Redirect URI */
    public static final String REDIRECT_URI = "oauth2_redirect_uri";
    
    /** OAuth2 Scope */
    public static final String SCOPE = "oauth2_scope";
    
    /** OAuth2 State */
    public static final String STATE = "oauth2_state";
    
    /** 外部認證 Session ID */
    public static final String EXTERNAL_SESSION = "oauth2_external_session";
    
    // ========== 使用者認證相關 ==========
    
    /** 使用者 Customer ID */
    public static final String CUSTOMER_ID = "customer_id";
    
    /** 認證時間 */
    public static final String AUTH_TIME = "auth_time";
    
    /** 外部系統 Token */
    public static final String EXTERNAL_TOKEN = "external_token";
}

