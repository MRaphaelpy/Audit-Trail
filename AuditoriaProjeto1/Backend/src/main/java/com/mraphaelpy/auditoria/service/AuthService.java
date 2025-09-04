package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.config.SecurityProperties;
import com.mraphaelpy.auditoria.dto.request.LoginRequest;
import com.mraphaelpy.auditoria.dto.request.TwoFactorRequest;
import com.mraphaelpy.auditoria.dto.response.LoginResponse;
import com.mraphaelpy.auditoria.entity.LoginAttempt;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.repository.LoginAttemptRepository;
import com.mraphaelpy.auditoria.repository.UserRepository;
import com.mraphaelpy.auditoria.service.audit.AuditoriaService;
import com.mraphaelpy.auditoria.service.logging.SecurityLoggingService;
import com.mraphaelpy.auditoria.service.logging.StructuredLoggingService;
import com.mraphaelpy.auditoria.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SecurityProperties securityProperties;
    private final AppConfig appConfig;
    private final SecurityLoggingService securityLoggingService;
    private final AuditoriaService auditoriaService;
    private final StructuredLoggingService structuredLoggingService;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginResponse authenticate(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Tentativa de login para email: {} de IP: {}", request.getEmail(), ipAddress);

        structuredLoggingService.logInfo("auth", "Tentativa de autenticação iniciada",
                Map.of("email", request.getEmail(), "ip_address", ipAddress, "user_agent", userAgent));

        HttpServletRequest httpRequest = getCurrentHttpRequest();

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Usuário não encontrado", false);
            securityLoggingService.logLoginFailed(request.getEmail(), httpRequest, "Usuário não encontrado");
            auditoriaService.loginFalha(request.getEmail(), ipAddress, userAgent, "Usuário não encontrado", 1);
            log.warn("Tentativa de login falhou - usuário não encontrado: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();
        }

        User user = userOpt.get();

        if (user.getAccountLocked() && user.getLockTime() != null) {
            if (user.getLockTime().isAfter(LocalDateTime.now())) {
                recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Conta bloqueada", false);
                auditoriaService.contaBloqueada(request.getEmail(), ipAddress, userAgent, user.getFailedAttempts());
                log.warn("Tentativa de login em conta bloqueada: {}", request.getEmail());
                boolean activeCaptcha = appConfig.isActiveCaptcha();
                if (activeCaptcha) {
                    Captcha generator = new Captcha();
                    Captcha.CaptchaData captcha;
                    try {
                        captcha = generator.generateCaptcha();
                    } catch (Exception e) {
                        log.error("Erro ao gerar captcha", e);
                        return LoginResponse.builder()
                                .success(false)
                                .message("Erro interno ao gerar captcha")
                                .requiresTwoFactor(false)
                                .build();
                    }

                    // salvar o texto na sessão para comparar depois
                    httpRequest.getSession().setAttribute("captcha", captcha.getText());

                    return LoginResponse.builder()
                            .success(false)
                            .message("Responda o captcha para fazer login novamente.")
                            .captchaImage(captcha.getImage())
                            .requiresTwoFactor(false)
                            .build();
                }
                return LoginResponse.builder()
                        .success(false)
                        .message("Conta temporariamente bloqueada. Tente novamente mais tarde.")
                        .requiresTwoFactor(false)
                        .build();
            } else {
                unlockAccount(user);
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return handleFailedLogin(user, request.getEmail(), ipAddress, userAgent, "Senha inválida");
        }

        resetFailedAttempts(user);

        if (appConfig.isTwoFactorEnabled()) {
            String twoFactorCode = generateTwoFactorCode();
            storeTwoFactorCode(user, twoFactorCode);

            try {
                emailService.sendTwoFactorCode(user.getEmail(), user.getUsername(), twoFactorCode);
            } catch (Exception e) {
                log.error("Falha ao enviar email 2FA, usando mock", e);
                emailService.mockSendTwoFactorCode(user.getEmail(), user.getUsername(), twoFactorCode);
            }

            String sessionToken = jwtUtil.generateSessionToken(user.getUsername());
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Aguardando 2FA", true);

            log.info("2FA obrigatório para usuário: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Código de verificação enviado para seu email")
                    .requiresTwoFactor(true)
                    .sessionToken(sessionToken)
                    .username(user.getUsername())
                    .build();
        }

        String token = jwtUtil.generateToken(user.getUsername());
        recordLoginAttempt(request.getEmail(), ipAddress, userAgent, true, "Login bem-sucedido", false);
        securityLoggingService.logLoginSuccess(request.getEmail(), httpRequest);
        auditoriaService.loginSucesso(request.getEmail(), ipAddress, userAgent, false);

        log.info("Login bem-sucedido para usuário: {}", request.getEmail());
        return LoginResponse.builder()
                .success(true)
                .message("Login realizado com sucesso")
                .token(token)
                .username(user.getUsername())
                .requiresTwoFactor(false)
                .build();
    }

    public LoginResponse verifyTwoFactor(TwoFactorRequest request, String ipAddress, String userAgent) {
        log.info("Verificação 2FA para usuário: {}", request.getEmail());

        if (!jwtUtil.isTokenValid(request.getSession_token()) || !jwtUtil.isSessionToken(request.getSession_token())) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Token de sessão inválido", false);
            log.warn("Token de sessão inválido para usuário: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Token de sessão inválido ou expirado")
                    .requiresTwoFactor(false)
                    .build();
        }

        String tokenUsername = jwtUtil.extractUsername(request.getSession_token());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Usuário não encontrado", false);
            log.warn("Usuário não encontrado durante verificação 2FA: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();
        }

        User user = userOpt.get();

        if (!tokenUsername.equals(user.getUsername())) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Usuário não corresponde ao token",
                    false);
            log.warn("Usuário não corresponde ao token de sessão: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();
        }

        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiry() == null) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Código 2FA não encontrado", false);
            log.warn("Código 2FA não encontrado para usuário: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Código de verificação não encontrado. Faça login novamente.")
                    .requiresTwoFactor(false)
                    .build();
        }

        if (user.getTwoFactorCodeExpiry().isBefore(LocalDateTime.now())) {
            clearTwoFactorCode(user);
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Código 2FA expirado", false);
            log.warn("Código 2FA expirado para usuário: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Código de verificação expirado. Faça login novamente.")
                    .requiresTwoFactor(false)
                    .build();
        }

        if (!user.getTwoFactorCode().equals(request.getCode())) {
            recordLoginAttempt(request.getEmail(), ipAddress, userAgent, false, "Código 2FA inválido", false);
            log.warn("Código 2FA inválido para usuário: {}", request.getEmail());
            return LoginResponse.builder()
                    .success(false)
                    .message("Código de verificação inválido")
                    .requiresTwoFactor(false)
                    .build();
        }

        clearTwoFactorCode(user);
        String token = jwtUtil.generateToken(user.getUsername());
        recordLoginAttempt(request.getEmail(), ipAddress, userAgent, true, "Login completo com 2FA", false);
        auditoriaService.loginSucesso(request.getEmail(), ipAddress, userAgent, true);

        log.info("Login completo com 2FA para usuário: {}", request.getEmail());
        return LoginResponse.builder()
                .success(true)
                .message("Login realizado com sucesso")
                .token(token)
                .username(user.getUsername())
                .requiresTwoFactor(false)
                .build();
    }

    private LoginResponse handleFailedLogin(User user, String email, String ipAddress, String userAgent,
            String reason) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);

        if (appConfig.isInfiniteAttemptsEnabled()) {
            userRepository.save(user);
            recordLoginAttempt(email, ipAddress, userAgent, false, reason, false);
            auditoriaService.loginFalha(email, ipAddress, userAgent, reason, user.getFailedAttempts());
            log.warn("Tentativa de login falhou ({}): {} - {} falhas (tentativas infinitas ativadas)",
                    reason, email, user.getFailedAttempts());
            return LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();
        }

        boolean shouldLock = appConfig.isAccountLockEnabled() &&
                user.getFailedAttempts() >= appConfig.getAccountLockMaxAttempts();
        if (shouldLock) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now().plusMinutes(appConfig.getAccountLockDurationMinutes()));
            userRepository.save(user);
            recordLoginAttempt(email, ipAddress, userAgent, false, reason + " - Conta bloqueada", false);
            auditoriaService.contaBloqueada(email, ipAddress, userAgent, user.getFailedAttempts());
            log.warn("Conta bloqueada após {} tentativas inválidas: {}", user.getFailedAttempts(), email);

            return LoginResponse.builder()
                    .success(false)
                    .message("Muitas tentativas inválidas. Conta temporariamente bloqueada.")
                    .requiresTwoFactor(false)
                    .build();
        } else {
            userRepository.save(user);
            recordLoginAttempt(email, ipAddress, userAgent, false, reason, false);
            auditoriaService.loginFalha(email, ipAddress, userAgent, reason, user.getFailedAttempts());
            log.warn("Tentativa de login falhou ({}): {} - {} falhas", reason, email, user.getFailedAttempts());
            return LoginResponse.builder()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();
        }
    }

    private String generateTwoFactorCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void storeTwoFactorCode(User user, String code) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(appConfig.getTwoFactorCodeExpiryMinutes());
        userRepository.updateTwoFactorCode(user.getUsername(), code, expiry);
    }

    private void clearTwoFactorCode(User user) {
        userRepository.clearTwoFactorCode(user.getUsername());
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    private void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Conta desbloqueada automaticamente: {}", user.getUsername());
    }

    private void recordLoginAttempt(String identifier, String ipAddress, String userAgent, boolean successful,
            String failureReason, boolean twoFactorRequired) {
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

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            log.debug("Não foi possível obter HttpServletRequest atual", e);
            return null;
        }
    }
}
