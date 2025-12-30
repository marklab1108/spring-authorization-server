package com.example.demo.repository;

import com.example.demo.entity.ConsentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Consent History Repository
 * 
 * 用於操作 oauth2_consent_history 表。
 */
@Repository
public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {

    /**
     * 依用戶查詢同意歷史
     */
    List<ConsentHistory> findByPrincipalNameOrderByConsentTimeDesc(String principalName);

    /**
     * 依 Client 查詢同意歷史
     */
    List<ConsentHistory> findByRegisteredClientIdOrderByConsentTimeDesc(String registeredClientId);

    /**
     * 依用戶和 Client 查詢同意歷史
     */
    List<ConsentHistory> findByRegisteredClientIdAndPrincipalNameOrderByConsentTimeDesc(
            String registeredClientId, String principalName);

    /**
     * 清理超過指定時間的歷史記錄
     * 
     * @param cutoffTime 截止時間，早於此時間的記錄將被刪除
     * @return 刪除的記錄數
     */
    @Modifying
    @Query("DELETE FROM ConsentHistory c WHERE c.consentTime < :cutoffTime")
    int deleteByConsentTimeBefore(@Param("cutoffTime") Instant cutoffTime);

    /**
     * 統計特定用戶的同意次數
     */
    long countByPrincipalName(String principalName);

    /**
     * 統計特定 Client 的同意次數
     */
    long countByRegisteredClientId(String registeredClientId);
}

