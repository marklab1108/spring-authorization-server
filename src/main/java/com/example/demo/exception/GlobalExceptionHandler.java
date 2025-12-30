package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全域例外處理器
 * 
 * 統一處理 Controller 層拋出的例外，避免敏感資訊洩漏。
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 處理認證相關例外
     */
    @ExceptionHandler(AuthException.class)
    public String handleAuthException(AuthException ex, Model model) {
        logger.warn("Authentication error [{}]: {}", ex.getErrorCode().name(), ex.getMessage());
        
        // 使用 enum 中定義的使用者友善訊息
        model.addAttribute("error", ex.getErrorCode().getUserMessage());
        return "error";
    }
    
    /**
     * 處理其他未預期的例外
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        // 記錄完整錯誤資訊供除錯使用，但不向使用者顯示
        logger.error("Unexpected error occurred", ex);
        
        model.addAttribute("error", ErrorCode.UNKNOWN.getUserMessage());
        return "error";
    }
}
