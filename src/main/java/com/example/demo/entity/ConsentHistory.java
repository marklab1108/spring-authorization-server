package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OAuth2 Consent History Entity
 * 
 * 記錄每次用戶同意授權的歷史，用於審計與合規。
 * 每次授權同意都會新增一筆記錄（不會覆蓋舊記錄）。
 */
@Entity
@Table(name = "oauth2_consent_history", schema = "poc_spring_authorization_server")
@Data
@NoArgsConstructor
public class ConsentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OAuth2 Client 的內部 ID（對應 oauth2_registered_client.id）
     */
    @Column(name = "registered_client_id", nullable = false, length = 100)
    private String registeredClientId;

    /**
     * 用戶識別碼（通常是 customerId）
     */
    @Column(name = "principal_name", nullable = false, length = 200)
    private String principalName;

    /**
     * 授權的 scopes（以空格分隔）
     */
    @Column(name = "scopes", nullable = false, length = 1000)
    private String scopes;

    /**
     * 同意時間
     */
    @Column(name = "consent_time", nullable = false)
    private Instant consentTime;

    public ConsentHistory(String registeredClientId, String principalName, String scopes) {
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
        this.scopes = scopes;
        this.consentTime = Instant.now();
    }
}
