package com.example.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Info Response DTO
 * 
 * Represents user information returned from the mock external authentication system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    
    /**
     * API status code ("0000" = success)
     */
    private String statusCode;

    /**
     * API status description
     */
    private String statusDesc;

    /**
     * Customer ID (unique identifier)
     */
    private String customerId;
}
