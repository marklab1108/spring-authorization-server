package com.example.demo.controller;

import com.example.demo.constant.SessionKeys;
import com.example.demo.exception.AuthException;
import com.example.demo.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * Renders the custom terms/consent page used by Spring Authorization Server.
 * This is invoked via consentPage("/terms") in AuthorizationServerConfig.
 */
@Controller
public class TermsController {

    private static final Logger logger = LoggerFactory.getLogger(TermsController.class);
    private final RegisteredClientRepository clientRepository;

    public TermsController(RegisteredClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/terms")
    public String termsPage(
            @RequestParam(name = "client_id", required = false) String clientIdParam,
            @RequestParam(name = "state", required = false) String stateParam,
            @RequestParam(name = "scope", required = false) String scopeParam,
            HttpSession session,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthException(ErrorCode.NOT_AUTHENTICATED, "需要先完成登入");
        }

        // Use Apache Commons StringUtils.firstNonBlank for cleaner code
        String clientId = StringUtils.firstNonBlank(clientIdParam, (String) session.getAttribute(SessionKeys.CLIENT_ID));
        String state = StringUtils.firstNonBlank(stateParam, (String) session.getAttribute(SessionKeys.STATE));
        String scope = StringUtils.firstNonBlank(scopeParam, (String) session.getAttribute(SessionKeys.SCOPE));

        if (StringUtils.isBlank(clientId)) {
            logger.warn("Missing client_id on /terms");
            throw new AuthException(ErrorCode.MISSING_CLIENT_ID, "授權請求資訊遺失（client_id）");
        }
        if (StringUtils.isBlank(state)) {
            logger.warn("Missing state on /terms");
            throw new AuthException(ErrorCode.MISSING_STATE, "授權請求資訊遺失（state）");
        }

        RegisteredClient client = clientRepository.findByClientId(clientId);
        String clientName = client != null ? client.getClientName() : clientId;

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", clientName);
        model.addAttribute("state", state);

        if (StringUtils.isNotBlank(scope)) {
            List<String> scopes = Arrays.asList(scope.trim().split("\\s+"));
            model.addAttribute("scopes", scopes);
        } else {
            model.addAttribute("scopes", List.of());
        }

        model.addAttribute("customerId", session.getAttribute(SessionKeys.CUSTOMER_ID));
        model.addAttribute("redirectUri", session.getAttribute(SessionKeys.REDIRECT_URI));
        
        logger.info("Showing terms page for client: {}", clientId);
        return "terms";
    }
}
