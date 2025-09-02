package com.mraphaelpy.auditoria.entity.logs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs", indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_event_type", columnList = "eventType"),
        @Index(name = "idx_user_identifier", columnList = "userIdentifier"),
        @Index(name = "idx_ip_address", columnList = "ipAddress"),
        @Index(name = "idx_risk_level", columnList = "riskLevel")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "resource_accessed", length = 500)
    private String resourceAccessed;

    @Column(name = "permission_required", length = 100)
    private String permissionRequired;

    @Column(name = "additional_data", columnDefinition = "JSON")
    private String additionalData;

    @Column(name = "risk_level", length = 20)
    @Builder.Default
    private String riskLevel = "LOW";

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
