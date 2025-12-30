package com.example.demo.exception;

/**
 * 認證錯誤碼列舉
 * 
 * 定義所有認證相關的錯誤碼與對應的使用者訊息。
 */
public enum ErrorCode {
    
    /** 缺少授權請求 */
    MISSING_AUTH_REQUEST("找不到授權請求，請重新發起授權流程"),
    
    /** 缺少 Client ID */
    MISSING_CLIENT_ID("授權請求缺少 client_id，請重新發起授權流程"),
    
    /** 缺少 State */
    MISSING_STATE("授權請求資訊遺失（state），請重新發起授權流程"),
    
    /** 外部認證失敗 */
    EXTERNAL_AUTH_FAILED("外部認證失敗，請重試"),
    
    /** Session 驗證失敗 */
    SESSION_VALIDATION_FAILED("Session 驗證失敗，請重新發起授權流程"),
    
    /** 授權流程過期 */
    AUTH_FLOW_EXPIRED("授權流程已過期或無效，請重新發起授權流程"),
    
    /** 外部 API 呼叫失敗 */
    EXTERNAL_API_FAILED("外部服務暫時無法使用，請稍後再試"),
    
    /** 缺少使用者代號 */
    MISSING_CUSTOMER_ID("無法取得使用者資訊，請重試"),
    
    /** 未登入 */
    NOT_AUTHENTICATED("需要先完成登入才能檢視授權條款頁"),
    
    /** 回調資料解析失敗 */
    CALLBACK_PARSE_FAILED("回調資料格式錯誤，請重新發起授權流程"),
    
    /** 未知錯誤 */
    UNKNOWN("系統發生錯誤，請稍後再試");

    private final String userMessage;

    ErrorCode(String userMessage) {
        this.userMessage = userMessage;
    }

    /**
     * 取得使用者友善的錯誤訊息
     */
    public String getUserMessage() {
        return userMessage;
    }
}

