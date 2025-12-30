package com.example.demo.controller;

import com.example.demo.constant.SessionKeys;
import com.example.demo.dto.ExternalAuthCallbackDto;
import com.example.demo.dto.ExternalUserInfoResponse;
import com.example.demo.exception.AuthException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.ExternalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

/**
 * Handles callbacks from external authentication system.
 *
 * Expected query parameter:
 * - data: Base64 encoded JSON {statusCode,statusDesc,session,token}
 */
@Controller
public class ExternalAuthCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(ExternalAuthCallbackController.class);

    private final ExternalAuthService externalAuthService;
    private final HttpSessionRequestCache requestCache;

    public ExternalAuthCallbackController(
            ExternalAuthService externalAuthService,
            HttpSessionRequestCache requestCache) {
        this.externalAuthService = externalAuthService;
        this.requestCache = requestCache;
    }

    @GetMapping("/oauth2/callback")
    public String handleCallback(
            @RequestParam("data") String base64Data,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) {

        logger.info("Received callback from external auth system");

        // Step 1: Parse and validate callback data
        ExternalAuthCallbackDto callbackDto = parseAndValidateCallback(base64Data);

        // Step 2: Validate session
        SessionContext sessionContext = validateSession(session, callbackDto);

        // Step 3: Get user info from external API
        String customerId = fetchUserInfo(callbackDto.token());

        // Step 4: Establish authentication
        establishAuthentication(session, callbackDto, customerId, request, response);

        // Step 5: Redirect to original request
        return buildRedirectUrl(sessionContext, request, response);
    }

    /**
     * Parse Base64 callback data and validate external auth status
     */
    private ExternalAuthCallbackDto parseAndValidateCallback(String base64Data) {
        ExternalAuthCallbackDto callbackDto = externalAuthService.parseCallbackData(base64Data);
        logger.debug("Parsed callback data successfully");

        if (!externalAuthService.isSuccessStatusCode(callbackDto.statusCode())) {
            throw new AuthException(ErrorCode.EXTERNAL_AUTH_FAILED, 
                    "外部認證失敗：" + callbackDto.statusDesc());
        }

        if (callbackDto.token() == null || callbackDto.token().isBlank()) {
            throw new AuthException(ErrorCode.EXTERNAL_AUTH_FAILED, "外部認證缺少 token");
        }

        return callbackDto;
    }

    /**
     * Validate session consistency between stored and callback data
     */
    private SessionContext validateSession(HttpSession session, ExternalAuthCallbackDto callbackDto) {
        String clientId = (String) session.getAttribute(SessionKeys.CLIENT_ID);
        String redirectUri = (String) session.getAttribute(SessionKeys.REDIRECT_URI);
        String scope = (String) session.getAttribute(SessionKeys.SCOPE);
        String state = (String) session.getAttribute(SessionKeys.STATE);
        String expectedSession = (String) session.getAttribute(SessionKeys.EXTERNAL_SESSION);

        if (clientId == null || clientId.isBlank() || expectedSession == null || expectedSession.isBlank()) {
            throw new AuthException(ErrorCode.AUTH_FLOW_EXPIRED, "授權流程已過期或無效");
        }

        if (!externalAuthService.validateSession(callbackDto.session(), clientId)) {
            throw new AuthException(ErrorCode.SESSION_VALIDATION_FAILED, "Session 格式不正確");
        }

        if (!expectedSession.equals(callbackDto.session())) {
            throw new AuthException(ErrorCode.SESSION_VALIDATION_FAILED, "Session 不符合預期");
        }

        return new SessionContext(clientId, redirectUri, scope, state);
    }

    /**
     * Fetch user info from external API
     */
    private String fetchUserInfo(String token) {
        ExternalUserInfoResponse userInfo = externalAuthService.getUserInfo(token);
        
        if (!externalAuthService.isSuccessStatusCode(userInfo.statusCode())) {
            throw new AuthException(ErrorCode.EXTERNAL_API_FAILED, 
                    "外部 API 失敗：" + userInfo.statusDesc());
        }

        String customerId = userInfo.customerId();
        if (customerId == null || customerId.isBlank()) {
            throw new AuthException(ErrorCode.MISSING_CUSTOMER_ID, "外部 API 未回傳使用者代號");
        }

        logger.info("User authenticated successfully");
        return customerId;
    }

    /**
     * Establish Spring Security authentication context
     */
    private void establishAuthentication(
            HttpSession session,
            ExternalAuthCallbackDto callbackDto,
            String customerId,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Store user info in session
        session.setAttribute(SessionKeys.CUSTOMER_ID, customerId);
        session.setAttribute(SessionKeys.AUTH_TIME, Instant.now());
        session.setAttribute(SessionKeys.EXTERNAL_SESSION, callbackDto.session());
        session.setAttribute(SessionKeys.EXTERNAL_TOKEN, callbackDto.token());

        // Create and persist authentication
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(customerId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);

        // Clean up the expected session after successful validation
        session.removeAttribute(SessionKeys.EXTERNAL_SESSION);
    }

    /**
     * Build redirect URL to continue OAuth2 flow
     */
    private String buildRedirectUrl(
            SessionContext sessionContext,
            HttpServletRequest request,
            HttpServletResponse response) {

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        requestCache.removeRequest(request, response);

        String target = savedRequest != null ? savedRequest.getRedirectUrl() : null;
        if (target == null || target.contains("/error")) {
            target = rebuildAuthorizeUrl(sessionContext);
            logger.warn("Saved request missing/invalid. Rebuilt authorize URL");
        } else {
            logger.info("Redirecting back to saved request");
        }

        return "redirect:" + target;
    }

    /**
     * Rebuild OAuth2 authorize URL from session context
     */
    private String rebuildAuthorizeUrl(SessionContext ctx) {
        StringBuilder sb = new StringBuilder("/oauth2/authorize?response_type=code");
        sb.append("&client_id=").append(urlEncode(ctx.clientId));
        if (ctx.redirectUri != null && !ctx.redirectUri.isBlank()) {
            sb.append("&redirect_uri=").append(urlEncode(ctx.redirectUri));
        }
        if (ctx.scope != null && !ctx.scope.isBlank()) {
            sb.append("&scope=").append(urlEncode(ctx.scope));
        }
        if (ctx.state != null && !ctx.state.isBlank()) {
            sb.append("&state=").append(urlEncode(ctx.state));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Internal record to hold session context data
     */
    private record SessionContext(String clientId, String redirectUri, String scope, String state) {}
}
