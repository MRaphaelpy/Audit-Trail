package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.repository.UserRepository;
import com.mraphaelpy.auditoria.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AppConfig appConfig;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

        public boolean isTwoFactorEnabled() {
        return appConfig.isTwoFactorEnabled();
    }

        public String generateAndSendTwoFactorCode(User user) {
        String twoFactorCode = generateTwoFactorCode();
        storeTwoFactorCode(user, twoFactorCode);

        try {
            emailService.sendTwoFactorCode(user.getEmail(), user.getUsername(), twoFactorCode);
            log.info("Código 2FA enviado com sucesso para: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Falha ao enviar email 2FA, usando mock", e);
            emailService.mockSendTwoFactorCode(user.getEmail(), user.getUsername(), twoFactorCode);
        }

        return jwtUtil.generateSessionToken(user.getUsername());
    }

        public boolean validateTwoFactorCode(User user, String providedCode, String sessionToken) {

        if (!jwtUtil.isTokenValid(sessionToken) || !jwtUtil.isSessionToken(sessionToken)) {
            log.warn("Token de sessão inválido para usuário: {}", user.getEmail());
            throw new TwoFactorValidationException("Token de sessão inválido ou expirado");
        }

        String tokenUsername = jwtUtil.extractUsername(sessionToken);
        if (!tokenUsername.equals(user.getUsername())) {
            log.warn("Usuário não corresponde ao token de sessão: {}", user.getEmail());
            throw new TwoFactorValidationException("Credenciais inválidas");
        }

        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiry() == null) {
            log.warn("Código 2FA não encontrado para usuário: {}", user.getEmail());
            throw new TwoFactorValidationException("Código de verificação não encontrado. Faça login novamente.");
        }

        if (user.getTwoFactorCodeExpiry().isBefore(LocalDateTime.now())) {
            clearTwoFactorCode(user);
            log.warn("Código 2FA expirado para usuário: {}", user.getEmail());
            throw new TwoFactorValidationException("Código de verificação expirado. Faça login novamente.");
        }

        if (!user.getTwoFactorCode().equals(providedCode)) {
            log.warn("Código 2FA inválido para usuário: {}", user.getEmail());
            throw new TwoFactorValidationException("Código de verificação inválido");
        }

        clearTwoFactorCode(user);
        log.info("Código 2FA validado com sucesso para usuário: {}", user.getEmail());
        return true;
    }

        private String generateTwoFactorCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

        private void storeTwoFactorCode(User user, String code) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(appConfig.getTwoFactorCodeExpiryMinutes());
        userRepository.updateTwoFactorCode(user.getUsername(), code, expiry);
        log.debug("Código 2FA armazenado para usuário: {} (expira em: {})", user.getUsername(), expiry);
    }

        private void clearTwoFactorCode(User user) {
        userRepository.clearTwoFactorCode(user.getUsername());
        log.debug("Código 2FA removido para usuário: {}", user.getUsername());
    }

        public static class TwoFactorValidationException extends RuntimeException {
        public TwoFactorValidationException(String message) {
            super(message);
        }
    }
}
