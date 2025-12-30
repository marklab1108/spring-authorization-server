package com.example.demo.dto;

/**
 * External User Information API Request
 * 
 * Request payload for calling external system's user info API.
 * 
 * @param platformId Platform identifier (e.g., "authserver")
 * @param token Token returned by external authentication system
 */
public record ExternalUserInfoRequest(
    String platformId,
    String token
) {
    @Override
    public String toString() {
        return "ExternalUserInfoRequest{" +
                "platformId='" + platformId + '\'' +
                ", token='***'" +
                '}';
    }
}
