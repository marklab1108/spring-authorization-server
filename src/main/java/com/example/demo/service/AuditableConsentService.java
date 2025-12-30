package com.example.demo.service;

import com.example.demo.entity.ConsentHistory;
import com.example.demo.repository.ConsentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Auditable OAuth2 Authorization Consent Service
 * 
 * 自訂的 Consent Service，實現以下功能：
 * 1. 每次授權都需要用戶重新同意（findById 總是回傳 null）
 * 2. 每次同意都記錄到歷史表（oauth2_consent_history）供審計使用
 * 
 * 注意：此實作不使用 Spring SAS 原生的 oauth2_authorization_consent 表，
 *       而是使用自訂的 oauth2_consent_history 表來保存軌跡。
 */
@Service
public class AuditableConsentService implements OAuth2AuthorizationConsentService {

    private static final Logger logger = LoggerFactory.getLogger(AuditableConsentService.class);

    private final ConsentHistoryRepository consentHistoryRepository;

    public AuditableConsentService(ConsentHistoryRepository consentHistoryRepository) {
        this.consentHistoryRepository = consentHistoryRepository;
    }

    /**
     * 儲存授權同意
     * 
     * 將同意記錄寫入歷史表，用於審計追蹤。
     * 不會寫入 Spring SAS 原生的 oauth2_authorization_consent 表。
     */
    @Override
    @Transactional
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        String registeredClientId = authorizationConsent.getRegisteredClientId();
        String principalName = authorizationConsent.getPrincipalName();
        
        // 將 authorities (GrantedAuthority) 轉換為 scopes 字串
        String scopes = authorizationConsent.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("SCOPE_"))
                .map(auth -> auth.substring(6)) // 移除 "SCOPE_" 前綴
                .collect(Collectors.joining(" "));

        // 寫入歷史記錄
        ConsentHistory history = new ConsentHistory(registeredClientId, principalName, scopes);
        consentHistoryRepository.save(history);

        logger.info("Consent history recorded for client: {}, user: {}", 
                registeredClientId, principalName);
    }

    /**
     * 移除授權同意
     * 
     * 由於我們不使用原生 consent 表，此方法不需要做任何事。
     * 歷史記錄會保留供審計使用，不會被刪除。
     */
    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        // 不刪除歷史記錄，保留供審計使用
        logger.debug("Remove consent called but ignored (history preserved) for client: {}, user: {}",
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName());
    }

    /**
     * 查詢授權同意
     * 
     * 總是回傳 null，確保每次授權都需要用戶重新同意條款。
     * 這實現了「每次授權都要顯示條款頁」的需求。
     */
    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        // 總是回傳 null，強制每次都要同意
        logger.debug("Consent lookup for client: {}, user: {} - returning null to force consent",
                registeredClientId, principalName);
        return null;
    }
}

