package com.example.demo.dto;

/**
 * External User Information API Response
 * 
 * Response from external user info API.
 * 
 * @param statusCode API status code ("0000" = success)
 * @param statusDesc API status description
 * @param customerId User's customer ID (e.g., national ID)
 */
public record ExternalUserInfoResponse(
    String statusCode,
    String statusDesc,
    String customerId
) {
    /**
     * Check if API call was successful
     * 
     * @return true if statusCode is "0000"
     */
    public boolean isSuccess() {
        return "0000".equals(statusCode);
    }
}
