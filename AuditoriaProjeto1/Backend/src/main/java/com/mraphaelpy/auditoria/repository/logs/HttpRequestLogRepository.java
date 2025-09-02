package com.mraphaelpy.auditoria.repository.logs;

import com.mraphaelpy.auditoria.entity.logs.HttpRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HttpRequestLogRepository extends JpaRepository<HttpRequestLog, Long> {

    List<HttpRequestLog> findByMethodOrderByTimestampDesc(String method);

    List<HttpRequestLog> findByUserIdOrderByTimestampDesc(String userId);

    List<HttpRequestLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    List<HttpRequestLog> findByResponseStatusOrderByTimestampDesc(Integer responseStatus);

    @Query("SELECT h FROM HttpRequestLog h WHERE h.uri LIKE %:uriPattern% ORDER BY h.timestamp DESC")
    List<HttpRequestLog> findByUriContaining(@Param("uriPattern") String uriPattern);

    @Query("SELECT h FROM HttpRequestLog h WHERE h.timestamp BETWEEN :startTime AND :endTime ORDER BY h.timestamp DESC")
    List<HttpRequestLog> findByTimestampBetween(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT h FROM HttpRequestLog h WHERE h.responseStatus >= 400 ORDER BY h.timestamp DESC")
    List<HttpRequestLog> findErrorRequests();

    @Query("SELECT AVG(h.processingTimeMs) FROM HttpRequestLog h WHERE h.timestamp > :since")
    Double getAverageProcessingTime(@Param("since") LocalDateTime since);
}
