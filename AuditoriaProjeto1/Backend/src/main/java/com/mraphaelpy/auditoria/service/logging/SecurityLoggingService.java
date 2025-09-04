package com.mraphaelpy.auditoria.service.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mraphaelpy.auditoria.entity.logs.SecurityLog;
import com.mraphaelpy.auditoria.repository.logs.SecurityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityLoggingService {

    private final SecurityLogRepository securityLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

        public void logLoginSuccess(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("LOGIN_SUCCESS", userIdentifier, request, null, "LOW", null);
    }

        public void logLoginFailed(String userIdentifier, HttpServletRequest request, String reason) {
        logSecurityEvent("LOGIN_FAILED", userIdentifier, request, null, "MEDIUM",
                Map.of("failureReason", reason));
    }

        public void logAccountLocked(String userIdentifier, HttpServletRequest request, int attempts) {
        logSecurityEvent("ACCOUNT_LOCKED", userIdentifier, request, null, "HIGH",
                Map.of("failedAttempts", attempts));
    }

        public void logAccessDenied(String userIdentifier, HttpServletRequest request, String resource) {
        logSecurityEvent("ACCESS_DENIED", userIdentifier, request, resource, "MEDIUM", null);
    }

        public void logUnauthorizedAccess(HttpServletRequest request, String resource) {
        logSecurityEvent("UNAUTHORIZED_ACCESS", null, request, resource, "HIGH", null);
    }

        public void logTwoFactorUsed(String userIdentifier, HttpServletRequest request, boolean success) {
        String eventType = success ? "TWO_FACTOR_SUCCESS" : "TWO_FACTOR_FAILED";
        logSecurityEvent(eventType, userIdentifier, request, null, "MEDIUM", null);
    }

        public void logPasswordChange(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("PASSWORD_CHANGED", userIdentifier, request, null, "MEDIUM", null);
    }

        public void logBruteForceAttempt(String userIdentifier, HttpServletRequest request, int attempts) {
        logSecurityEvent("BRUTE_FORCE_ATTEMPT", userIdentifier, request, null, "CRITICAL",
                Map.of("attempts", attempts));
    }

        public void logInvalidToken(HttpServletRequest request, String reason) {
        logSecurityEvent("INVALID_TOKEN", null, request, null, "MEDIUM",
                Map.of("reason", reason));
    }

        public void logSessionExpired(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("SESSION_EXPIRED", userIdentifier, request, null, "LOW", null);
    }

        public void logSecurityEvent(String eventType, String userIdentifier, HttpServletRequest request,
            String resource, String riskLevel, Map<String, Object> additionalData) {

        log.info("SECURITY_EVENT: {} - User: {} - Resource: {} - Risk: {} - IP: {}",
                eventType, userIdentifier, resource, riskLevel, getClientIpAddress(request));

        CompletableFuture.runAsync(() -> {
            try {
                SecurityLog securityLog = SecurityLog.builder()
                        .eventType(eventType)
                        .userIdentifier(userIdentifier)
                        .ipAddress(getClientIpAddress(request))
                        .userAgent(request != null ? request.getHeader("User-Agent") : null)
                        .resourceAccessed(resource)
                        .riskLevel(riskLevel)
                        .additionalData(additionalData != null ? objectMapper.writeValueAsString(additionalData) : null)
                        .timestamp(LocalDateTime.now())
                        .build();

                securityLogRepository.save(securityLog);

            } catch (Exception e) {
                log.error("Erro ao salvar log de segurança no banco", e);
            }
        });
    }

        public void checkHighRiskEvents() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            long bruteForceAttempts = securityLogRepository.countEventsSince("BRUTE_FORCE_ATTEMPT", oneHourAgo);
            if (bruteForceAttempts > 10) {
                log.warn("ALERTA: {} tentativas de força bruta na última hora", bruteForceAttempts);
            }

            long unauthorizedAttempts = securityLogRepository.countEventsSince("UNAUTHORIZED_ACCESS", oneHourAgo);
            if (unauthorizedAttempts > 20) {
                log.warn("ALERTA: {} tentativas de acesso não autorizado na última hora", unauthorizedAttempts);
            }

        } catch (Exception e) {
            log.error("Erro ao verificar eventos de alto risco", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null)
            return "UNKNOWN";

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}
