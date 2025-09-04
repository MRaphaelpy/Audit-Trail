package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.repository.UserRepository;
import com.mraphaelpy.auditoria.service.audit.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockService {

    private final UserRepository userRepository;
    private final AuditoriaService auditoriaService;
    private final AppConfig appConfig;

  
    public void checkAccountStatus(User user, String ipAddress, String userAgent) {
        if (user.getAccountLocked() && user.getLockTime() != null) {
            if (user.getLockTime().isAfter(LocalDateTime.now())) {
                auditoriaService.contaBloqueada(user.getEmail(), ipAddress, userAgent, user.getFailedAttempts());
                log.warn("Tentativa de login em conta bloqueada: {}", user.getEmail());
                throw new AccountLockedException("Conta temporariamente bloqueada");
            } else {
                unlockAccount(user);
            }
        }
    }


    public void handleFailedAttempt(User user, String email, String ipAddress, String userAgent, String reason) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);

        if (appConfig.isInfiniteAttemptsEnabled()) {
            userRepository.save(user);
            auditoriaService.loginFalha(email, ipAddress, userAgent, reason, user.getFailedAttempts());
            log.warn("Tentativa de login falhou ({}): {} - {} falhas (tentativas infinitas ativadas)",
                    reason, email, user.getFailedAttempts());
            return;
        }

        boolean shouldLock = appConfig.isAccountLockEnabled() &&
                user.getFailedAttempts() >= appConfig.getAccountLockMaxAttempts();

        if (shouldLock) {
            lockAccount(user, email, ipAddress, userAgent, reason);
            throw new AccountLockedException("Muitas tentativas inválidas. Conta temporariamente bloqueada.");
        } else {
            userRepository.save(user);
            auditoriaService.loginFalha(email, ipAddress, userAgent, reason, user.getFailedAttempts());
            log.warn("Tentativa de login falhou ({}): {} - {} falhas", reason, email, user.getFailedAttempts());
        }
    }


    public void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
            log.debug("Tentativas falhadas resetadas para usuário: {}", user.getUsername());
        }
    }
    public void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Conta desbloqueada automaticamente: {}", user.getUsername());
    }

    private void lockAccount(User user, String email, String ipAddress, String userAgent, String reason) {
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().plusMinutes(appConfig.getAccountLockDurationMinutes()));
        userRepository.save(user);
        auditoriaService.contaBloqueada(email, ipAddress, userAgent, user.getFailedAttempts());
        log.warn("Conta bloqueada após {} tentativas inválidas: {}", user.getFailedAttempts(), email);
    }

    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(String message) {
            super(message);
        }
    }
}
