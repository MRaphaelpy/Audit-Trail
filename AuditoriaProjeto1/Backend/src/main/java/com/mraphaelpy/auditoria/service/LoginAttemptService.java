package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.entity.LoginAttempt;
import com.mraphaelpy.auditoria.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

        public void recordLoginAttempt(String identifier, String ipAddress, String userAgent,
            boolean successful, String failureReason, boolean twoFactorRequired) {

        LoginAttempt attempt = LoginAttempt.builder()
                .username(identifier)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(successful)
                .failureReason(successful ? null : failureReason)
                .captchaRequired(false)
                .twoFactorRequired(twoFactorRequired)
                .attemptTime(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);

        if (successful) {
            log.info("✅ LOGIN SUCESSO - Usuário: {} | IP: {} | 2FA: {} | Hora: {}",
                    identifier, ipAddress, twoFactorRequired, LocalDateTime.now());
        } else {
            log.warn("❌ LOGIN FALHA - Usuário: {} | IP: {} | Motivo: {} | 2FA: {} | Hora: {}",
                    identifier, ipAddress, failureReason, twoFactorRequired, LocalDateTime.now());
        }
    }

        public void recordSuccessfulLogin(String identifier, String ipAddress, String userAgent, boolean twoFactorUsed) {
        recordLoginAttempt(identifier, ipAddress, userAgent, true, null, twoFactorUsed);
    }

        public void recordFailedLogin(String identifier, String ipAddress, String userAgent, String failureReason) {
        recordLoginAttempt(identifier, ipAddress, userAgent, false, failureReason, false);
    }
}
