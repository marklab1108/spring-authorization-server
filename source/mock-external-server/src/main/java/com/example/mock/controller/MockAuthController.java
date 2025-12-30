package com.example.mock.controller;

import com.example.mock.dto.UserInfoResponse;
import com.example.mock.service.MockAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Mock Authentication Controller
 *
 * Simulates an external authentication system with login page and user info API
 */
@Controller
public class MockAuthController {

    private static final Logger logger = LoggerFactory.getLogger(MockAuthController.class);
    
    private final MockAuthService authService;

    public MockAuthController(MockAuthService authService) {
        this.authService = authService;
    }

    /**
     * Display login page
     *
     * @param session External session identifier
     * @param callbackUrl Callback URL after successful login
     * @param model Spring MVC model
     * @return Template name
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam("session") String session,
            @RequestParam("callback_url") String callbackUrl,
            Model model) {

        logger.info("Mock login page requested");

        model.addAttribute("authSession", session);  // Avoid conflict with Thymeleaf's ${session}
        model.addAttribute("callbackUrl", callbackUrl);

        return "mock-login";
    }

    /**
     * Handle login form submission
     *
     * @param session External session identifier
     * @param callbackUrl Callback URL
     * @param customerId Customer ID from form
     * @return Redirect to callback URL
     */
    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("session") String session,
            @RequestParam("callback_url") String callbackUrl,
            @RequestParam("customer_id") String customerId) {

        logger.info("Mock login submitted for session");

        String token = authService.registerSession(session, customerId);
        String data = authService.buildCallbackData("0000", "OK", session, token);
        String redirectUrl = authService.buildRedirectUrl(callbackUrl, data);

        logger.info("Redirecting to callback URL");
        return "redirect:" + redirectUrl;
    }

    /**
     * Test login endpoint (auto-login without form)
     * Useful for automated testing
     */
    @GetMapping("/test-login")
    public String testLogin(
            @RequestParam("session") String session,
            @RequestParam("callback_url") String callbackUrl,
            @RequestParam(value = "customer_id", defaultValue = "test123") String customerId) {

        logger.info("Test login (auto) for session");

        String token = authService.registerSession(session, customerId);
        String data = authService.buildCallbackData("0000", "OK", session, token);
        String redirectUrl = authService.buildRedirectUrl(callbackUrl, data);

        logger.info("Redirecting to callback URL");
        return "redirect:" + redirectUrl;
    }

    /**
     * User info API endpoint
     *
     * Request: {"platformId":"authserver","token":"..."}
     * Response: {"statusCode":"0000|...","statusDesc":"...","customerId":"..."}
     */
    @PostMapping("/api/userinfo")
    @ResponseBody
    public UserInfoResponse getUserInfo(@RequestBody Map<String, String> request) {
        String platformId = request.get("platformId");
        String token = request.get("token");

        logger.info("User info requested for platformId: {}", platformId);

        if (platformId == null || token == null) {
            logger.error("Missing platformId or token");
            return new UserInfoResponse("9001", "缺少必要參數", null);
        }

        String customerId = authService.getCustomerIdByToken(token);
        if (customerId == null) {
            logger.error("Token not found or expired");
            return new UserInfoResponse("9002", "無效的 token", null);
        }

        logger.info("Returning user info successfully");
        return new UserInfoResponse("0000", "OK", customerId);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mock-external-server");
    }
}
