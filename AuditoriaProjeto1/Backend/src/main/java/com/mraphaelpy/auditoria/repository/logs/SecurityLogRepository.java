package com.mraphaelpy.auditoria.repository.logs;

import com.mraphaelpy.auditoria.entity.logs.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    List<SecurityLog> findByEventTypeOrderByTimestampDesc(String eventType);

    List<SecurityLog> findByUserIdentifierOrderByTimestampDesc(String userIdentifier);

    List<SecurityLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    List<SecurityLog> findByRiskLevelOrderByTimestampDesc(String riskLevel);

    @Query("SELECT s FROM SecurityLog s WHERE s.timestamp BETWEEN :startTime AND :endTime ORDER BY s.timestamp DESC")
    List<SecurityLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM SecurityLog s WHERE s.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY s.timestamp DESC")
    List<SecurityLog> findHighRiskEvents();

    @Query("SELECT COUNT(s) FROM SecurityLog s WHERE s.eventType = :eventType AND s.timestamp > :since")
    Long countEventsSince(@Param("eventType") String eventType, @Param("since") LocalDateTime since);
}
