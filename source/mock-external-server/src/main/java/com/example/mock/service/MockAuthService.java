package com.example.mock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Authentication Service
 * 
 * Business logic for mock authentication operations.
 * Manages session and token storage for the mock authentication flow.
 */
@Service
public class MockAuthService {

    private static final Logger logger = LoggerFactory.getLogger(MockAuthService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory session storage (session -> customer_id mapping)
    private final Map<String, String> sessionStore = new ConcurrentHashMap<>();
    
    // Token store (token -> customer_id) for API contract
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    /**
     * Register a user session and generate a token
     * 
     * @param session External session identifier
     * @param customerId Customer ID
     * @return Generated token
     */
    public String registerSession(String session, String customerId) {
        sessionStore.put(session, customerId);
        
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, customerId);
        
        logger.debug("Session registered: session={}", session);
        return token;
    }

    /**
     * Get customer ID by token
     * 
     * @param token Token to lookup
     * @return Customer ID or null if not found
     */
    public String getCustomerIdByToken(String token) {
        return tokenStore.get(token);
    }

    /**
     * Build callback data in Base64 encoded JSON format
     * 
     * @param statusCode Status code
     * @param statusDesc Status description
     * @param session Session identifier
     * @param token Token
     * @return Base64 encoded JSON string
     */
    public String buildCallbackData(String statusCode, String statusDesc, String session, String token) {
        Map<String, Object> payload = Map.of(
                "statusCode", statusCode,
                "statusDesc", statusDesc,
                "session", session,
                "token", token
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build callback data", e);
        }
    }

    /**
     * Build redirect URL with callback data
     * 
     * @param callbackUrl Base callback URL
     * @param data Callback data
     * @return Full redirect URL
     */
    public String buildRedirectUrl(String callbackUrl, String data) {
        return callbackUrl + "?data=" + urlEncode(data);
    }

    /**
     * URL encode a value
     */
    public String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
