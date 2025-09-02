package com.mraphaelpy.auditoria.repository.logs;

import com.mraphaelpy.auditoria.entity.logs.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {

    List<ApplicationLog> findByLevelOrderByTimestampDesc(String level);

    List<ApplicationLog> findByLoggerNameOrderByTimestampDesc(String loggerName);

    List<ApplicationLog> findByUserIdOrderByTimestampDesc(String userId);

    List<ApplicationLog> findByRequestIdOrderByTimestampDesc(String requestId);

    @Query("SELECT a FROM ApplicationLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<ApplicationLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM ApplicationLog a WHERE a.level = :level AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<ApplicationLog> findByLevelAndTimestampBetween(@Param("level") String level,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(a) FROM ApplicationLog a WHERE a.level = 'ERROR' AND a.timestamp > :since")
    Long countErrorsSince(@Param("since") LocalDateTime since);
}
