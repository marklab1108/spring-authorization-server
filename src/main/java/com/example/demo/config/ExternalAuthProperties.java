package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * External Authentication System Configuration Properties
 * 
 * Binds to external-auth.* properties in application.yaml
 */
@Component
@ConfigurationProperties(prefix = "external-auth")
@Data
public class ExternalAuthProperties {
    
    /**
     * External authentication system base URL
     */
    private String serverUrl = "http://localhost:8888";
    
    /**
     * Login endpoint path
     */
    private String loginEndpoint = "/login";
    
    /**
     * User info API endpoint
     */
    private String apiEndpoint = "/api/userinfo";
    
    /**
     * Platform ID for API calls
     */
    private String platformId = "authserver";

    /**
     * HTTP connect timeout (milliseconds)
     */
    private int connectTimeoutMs = 5000;

    /**
     * HTTP read timeout (milliseconds)
     */
    private int readTimeoutMs = 5000;
    
    /**
     * Get full login URL
     */
    public String getFullLoginUrl() {
        return serverUrl + loginEndpoint;
    }
    
    /**
     * Get full API URL
     */
    public String getFullApiUrl() {
        return serverUrl + apiEndpoint;
    }
}

