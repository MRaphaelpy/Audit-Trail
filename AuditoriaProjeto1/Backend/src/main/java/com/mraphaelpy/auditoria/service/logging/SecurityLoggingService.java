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

    /**
     * Log de evento de login bem-sucedido
     */
    public void logLoginSuccess(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("LOGIN_SUCCESS", userIdentifier, request, null, "LOW", null);
    }

    /**
     * Log de tentativa de login falhada
     */
    public void logLoginFailed(String userIdentifier, HttpServletRequest request, String reason) {
        logSecurityEvent("LOGIN_FAILED", userIdentifier, request, null, "MEDIUM",
                Map.of("failureReason", reason));
    }

    /**
     * Log de bloqueio de conta
     */
    public void logAccountLocked(String userIdentifier, HttpServletRequest request, int attempts) {
        logSecurityEvent("ACCOUNT_LOCKED", userIdentifier, request, null, "HIGH",
                Map.of("failedAttempts", attempts));
    }

    /**
     * Log de acesso negado
     */
    public void logAccessDenied(String userIdentifier, HttpServletRequest request, String resource) {
        logSecurityEvent("ACCESS_DENIED", userIdentifier, request, resource, "MEDIUM", null);
    }

    /**
     * Log de tentativa de acesso a recurso protegido
     */
    public void logUnauthorizedAccess(HttpServletRequest request, String resource) {
        logSecurityEvent("UNAUTHORIZED_ACCESS", null, request, resource, "HIGH", null);
    }

    /**
     * Log de uso de 2FA
     */
    public void logTwoFactorUsed(String userIdentifier, HttpServletRequest request, boolean success) {
        String eventType = success ? "TWO_FACTOR_SUCCESS" : "TWO_FACTOR_FAILED";
        logSecurityEvent(eventType, userIdentifier, request, null, "MEDIUM", null);
    }

    /**
     * Log de mudança de senha
     */
    public void logPasswordChange(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("PASSWORD_CHANGED", userIdentifier, request, null, "MEDIUM", null);
    }

    /**
     * Log de tentativa de força bruta
     */
    public void logBruteForceAttempt(String userIdentifier, HttpServletRequest request, int attempts) {
        logSecurityEvent("BRUTE_FORCE_ATTEMPT", userIdentifier, request, null, "CRITICAL",
                Map.of("attempts", attempts));
    }

    /**
     * Log de token JWT inválido
     */
    public void logInvalidToken(HttpServletRequest request, String reason) {
        logSecurityEvent("INVALID_TOKEN", null, request, null, "MEDIUM",
                Map.of("reason", reason));
    }

    /**
     * Log de sessão expirada
     */
    public void logSessionExpired(String userIdentifier, HttpServletRequest request) {
        logSecurityEvent("SESSION_EXPIRED", userIdentifier, request, null, "LOW", null);
    }

    /**
     * Log genérico de evento de segurança
     */
    public void logSecurityEvent(String eventType, String userIdentifier, HttpServletRequest request,
            String resource, String riskLevel, Map<String, Object> additionalData) {

        // Log no arquivo também
        log.info("SECURITY_EVENT: {} - User: {} - Resource: {} - Risk: {} - IP: {}",
                eventType, userIdentifier, resource, riskLevel, getClientIpAddress(request));

        // Salva no banco de forma assíncrona
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

    /**
     * Busca últimos eventos de segurança de alto risco
     */
    public void checkHighRiskEvents() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            // Verifica tentativas de força bruta
            long bruteForceAttempts = securityLogRepository.countEventsSince("BRUTE_FORCE_ATTEMPT", oneHourAgo);
            if (bruteForceAttempts > 10) {
                log.warn("ALERTA: {} tentativas de força bruta na última hora", bruteForceAttempts);
            }

            // Verifica acessos não autorizados
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
