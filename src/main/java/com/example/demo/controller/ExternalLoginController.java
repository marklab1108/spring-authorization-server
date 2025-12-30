package com.example.demo.controller;

import com.example.demo.constant.SessionKeys;
import com.example.demo.exception.AuthException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.ExternalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * External login entry point.
 * Users are sent here by Spring Security when unauthenticated during /oauth2/authorize.
 */
@Controller
public class ExternalLoginController {

    private static final Logger logger = LoggerFactory.getLogger(ExternalLoginController.class);

    private final RegisteredClientRepository clientRepository;
    private final ExternalAuthService externalAuthService;
    private final HttpSessionRequestCache requestCache;

    public ExternalLoginController(
            RegisteredClientRepository clientRepository, 
            ExternalAuthService externalAuthService,
            HttpSessionRequestCache requestCache) {
        this.clientRepository = clientRepository;
        this.externalAuthService = externalAuthService;
        this.requestCache = requestCache;
    }

    @GetMapping("/external-login")
    public String showExternalLogin(HttpServletRequest request, HttpServletResponse response, Model model) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            logger.warn("No saved request found in session for /external-login");
            throw new AuthException(ErrorCode.MISSING_AUTH_REQUEST, "找不到授權請求");
        }

        String clientId = getFirstParam(savedRequest, "client_id");
        if (clientId == null) {
            throw new AuthException(ErrorCode.MISSING_CLIENT_ID, "授權請求缺少 client_id");
        }

        String redirectUri = getFirstParam(savedRequest, "redirect_uri");
        String scope = getFirstParam(savedRequest, "scope");
        String state = getFirstParam(savedRequest, "state");

        RegisteredClient client = clientRepository.findByClientId(clientId);
        String clientName = client != null && client.getClientName() != null ? client.getClientName() : clientId;

        String authSession = UUID.randomUUID().toString();
        String externalSession = authSession + "_" + clientId;

        HttpSession session = request.getSession(true);
        session.setAttribute(SessionKeys.EXTERNAL_SESSION, externalSession);
        session.setAttribute(SessionKeys.CLIENT_ID, clientId);
        session.setAttribute(SessionKeys.REDIRECT_URI, redirectUri);
        session.setAttribute(SessionKeys.SCOPE, scope);
        session.setAttribute(SessionKeys.STATE, state);

        String callbackUrl = ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath("/oauth2/callback")
                .replaceQuery(null)
                .build()
                .toUriString();
        String externalLoginUrl = externalAuthService.buildExternalLoginUrl(externalSession, callbackUrl);

        logger.info("Redirecting to external login for client: {}", clientId);

        model.addAttribute("clientName", clientName);
        model.addAttribute("clientId", clientId);
        model.addAttribute("externalSession", externalSession);
        model.addAttribute("externalLoginUrl", externalLoginUrl);
        return "auth-home-page";
    }

    private String getFirstParam(SavedRequest savedRequest, String name) {
        String[] values = savedRequest.getParameterValues(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }
}
