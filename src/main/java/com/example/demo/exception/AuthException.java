package com.example.demo.exception;

/**
 * 認證相關例外
 * 
 * 用於標識認證流程中的各種錯誤情況。
 */
public class AuthException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public AuthException(ErrorCode errorCode) {
        super(errorCode.getUserMessage());
        this.errorCode = errorCode;
    }
    
    public AuthException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
    
    public AuthException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
