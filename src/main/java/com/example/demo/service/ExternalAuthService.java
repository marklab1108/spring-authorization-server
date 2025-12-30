package com.example.demo.service;

import com.example.demo.config.ExternalAuthProperties;
import com.example.demo.dto.ExternalAuthCallbackDto;
import com.example.demo.dto.ExternalUserInfoRequest;
import com.example.demo.dto.ExternalUserInfoResponse;
import com.example.demo.exception.AuthException;
import com.example.demo.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * External Authentication Service.
 */
@Service
public class ExternalAuthService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalAuthService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExternalAuthProperties properties;

    public ExternalAuthService(
            RestTemplate restTemplate, 
            ObjectMapper objectMapper,
            ExternalAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Parse Base64 encoded callback data from external authentication system
     */
    public ExternalAuthCallbackDto parseCallbackData(String base64Data) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            logger.debug("Decoded callback JSON");
            return objectMapper.readValue(jsonString, ExternalAuthCallbackDto.class);
        } catch (Exception e) {
            logger.error("Failed to parse callback data", e);
            throw new AuthException(ErrorCode.CALLBACK_PARSE_FAILED, "回調資料解析失敗", e);
        }
    }

    /**
     * Check if status code indicates success
     */
    public boolean isSuccessStatusCode(String statusCode) {
        return "0000".equals(statusCode);
    }

    /**
     * Validate session format and client ID
     * Session format: {authSession}_{clientId}
     */
    public boolean validateSession(String session, String expectedClientId) {
        if (session == null || session.isEmpty()) {
            logger.warn("Session is null or empty");
            return false;
        }

        String[] parts = session.split("_");
        if (parts.length < 2) {
            logger.warn("Invalid session format");
            return false;
        }

        String clientIdFromSession = parts[parts.length - 1];
        boolean valid = expectedClientId.equals(clientIdFromSession);
        if (!valid) {
            logger.warn("Session validation failed: client ID mismatch");
        }
        return valid;
    }

    /**
     * Call external API to get user information
     * 
     * External API contract:
     * Request: {"platformId":"authserver","token":"..."}
     * Response: {"statusCode":"0000|...","statusDesc":"...","customerId":"..."}
     */
    public ExternalUserInfoResponse getUserInfo(String externalToken) {
        ExternalUserInfoRequest request = new ExternalUserInfoRequest(properties.getPlatformId(), externalToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ExternalUserInfoRequest> entity = new HttpEntity<>(request, headers);

        String apiUrl = properties.getFullApiUrl();
        logger.debug("Calling external API");

        try {
            ResponseEntity<ExternalUserInfoResponse> responseEntity =
                    restTemplate.postForEntity(apiUrl, entity, ExternalUserInfoResponse.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new AuthException(ErrorCode.EXTERNAL_API_FAILED, 
                        "External API returned non-2xx status: " + responseEntity.getStatusCode());
            }

            ExternalUserInfoResponse body = responseEntity.getBody();
            if (body == null) {
                throw new AuthException(ErrorCode.EXTERNAL_API_FAILED, "External API returned empty body");
            }

            logger.debug("User info retrieved successfully");
            return body;
        } catch (HttpStatusCodeException e) {
            logger.error("External API returned error status: {}", e.getStatusCode());
            throw new AuthException(ErrorCode.EXTERNAL_API_FAILED, 
                    "External API returned error status: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            logger.error("External API call failed", e);
            throw new AuthException(ErrorCode.EXTERNAL_API_FAILED, 
                    "External API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build external login URL with session and callback parameters
     */
    public String buildExternalLoginUrl(String session, String callbackUri) {
        String url = UriComponentsBuilder
                .fromHttpUrl(properties.getFullLoginUrl())
                .queryParam("session", session)
                .queryParam("callback_url", callbackUri)
                .build()
                .toUriString();
        logger.debug("Built external login URL");
        return url;
    }
}
