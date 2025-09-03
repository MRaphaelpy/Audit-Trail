package com.mraphaelpy.auditoria.entity.logs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_logs", indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_level", columnList = "level"),
        @Index(name = "idx_logger", columnList = "loggerName"),
        @Index(name = "idx_request_id", columnList = "requestId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_ip_address", columnList = "ipAddress")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "logger_name", nullable = false)
    private String loggerName;

    @Column(name = "thread_name", nullable = false, length = 100)
    private String threadName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "exception_class")
    private String exceptionClass;

    @Column(name = "stack_trace", columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Column(name = "mdc_data", columnDefinition = "JSON", nullable = false)
    private String mdcData;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
