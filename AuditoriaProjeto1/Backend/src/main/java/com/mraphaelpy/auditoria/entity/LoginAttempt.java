package com.mraphaelpy.auditoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(nullable = false)
    private Boolean successful;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "captcha_required")
    @Builder.Default
    private Boolean captchaRequired = false;
    
    @Column(name = "two_factor_required")
    @Builder.Default
    private Boolean twoFactorRequired = false;
    
    @Column(name = "attempt_time")
    @Builder.Default
    private LocalDateTime attemptTime = LocalDateTime.now();
    
    @Column(name = "user_agent")
    private String userAgent;
}
