package com.mraphaelpy.auditoria.entity.logs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "http_request_logs", indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_method", columnList = "method"),
        @Index(name = "idx_uri", columnList = "uri"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_ip_address", columnList = "ipAddress"),
        @Index(name = "idx_status", columnList = "responseStatus")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(name = "query_string", length = 1000)
    private String queryString;

    @Column(columnDefinition = "JSON")
    private String headers;

    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "JSON")
    private String responseHeaders;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
