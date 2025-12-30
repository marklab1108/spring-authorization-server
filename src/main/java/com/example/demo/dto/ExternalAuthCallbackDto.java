package com.example.demo.dto;

/**
 * External Authentication Callback Data Transfer Object
 * 
 * Received from external authentication system after user authentication.
 * The data is Base64 encoded in the callback URL.
 * 
 * @param statusCode Authentication status code ("0000" = Success)
 * @param statusDesc Authentication status description
 * @param session Session identifier: {authorization_session}_{clientId}
 * @param token External system token for calling user info API
 */
public record ExternalAuthCallbackDto(
    String statusCode,
    String statusDesc,
    String session,
    String token
) {
    /**
     * Check if authentication was successful
     * 
     * @return true if statusCode is "0000"
     */
    public boolean isSuccess() {
        return "0000".equals(statusCode);
    }
    
    @Override
    public String toString() {
        return "ExternalAuthCallbackDto{" +
                "statusCode='" + statusCode + '\'' +
                ", statusDesc='" + statusDesc + '\'' +
                ", session='" + session + '\'' +
                ", token='" + (token != null ? "***" : "null") + '\'' +
                '}';
    }
}
