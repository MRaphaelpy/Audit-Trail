package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.dto.request.LoginRequest;
import com.mraphaelpy.auditoria.dto.request.TwoFactorRequest;
import com.mraphaelpy.auditoria.dto.response.LoginResponse;
import com.mraphaelpy.auditoria.entity.User;
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

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AppConfig appConfig;
    private final SecurityLoggingService securityLoggingService;
    private final AuditoriaService auditoriaService;
    private final StructuredLoggingService structuredLoggingService;

    
    private final UserValidationService userValidationService;
    private final AccountLockService accountLockService;
    private final TwoFactorService twoFactorService;
    private final LoginAttemptService loginAttemptService;

    private LoginResponse.LoginResponseBuilder createBaseLoginResponse() {
        return LoginResponse.builder()
                .captchaEnabled(appConfig.isActiveCaptcha())
                .twoFactorEnabled(appConfig.isTwoFactorEnabled());
    }

    public LoginResponse authenticate(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Tentativa de login para email: {} de IP: {}", request.getEmail(), ipAddress);

        structuredLoggingService.logInfo("auth", "Tentativa de autenticação iniciada",
                Map.of("email", request.getEmail(), "ip_address", ipAddress, "user_agent", userAgent));

        HttpServletRequest httpRequest = getCurrentHttpRequest();

        try {
            
            User user = userValidationService.validateAndGetUser(request.getEmail(), ipAddress, userAgent);

            
            accountLockService.checkAccountStatus(user, ipAddress, userAgent);

            
            validatePassword(request.getPassword(), user, request.getEmail(), ipAddress, userAgent);

            
            return processTwoFactorOrCompleteLogin(user, request.getEmail(), ipAddress, userAgent, httpRequest);

        } catch (UserValidationService.UserNotFoundException e) {
            loginAttemptService.recordFailedLogin(request.getEmail(), ipAddress, userAgent, "Usuário não encontrado");
            securityLoggingService.logLoginFailed(request.getEmail(), httpRequest, "Usuário não encontrado");

            return createBaseLoginResponse()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();

        } catch (AccountLockService.AccountLockedException e) {
            return handleAccountLocked(request.getEmail(), httpRequest);

        } catch (Exception e) {
            log.error("Erro inesperado durante autenticação", e);
            loginAttemptService.recordFailedLogin(request.getEmail(), ipAddress, userAgent, "Erro interno");

            return createBaseLoginResponse()
                    .success(false)
                    .message("Erro interno do servidor")
                    .requiresTwoFactor(false)
                    .build();
        }
    }

        private void validatePassword(String providedPassword, User user, String email, String ipAddress,
            String userAgent) {
        if (!passwordEncoder.matches(providedPassword, user.getPassword())) {
            accountLockService.handleFailedAttempt(user, email, ipAddress, userAgent, "Senha inválida");
            throw new RuntimeException("Senha inválida");
        }
    }

        private LoginResponse processTwoFactorOrCompleteLogin(User user, String email, String ipAddress, String userAgent,
            HttpServletRequest httpRequest) {
        accountLockService.resetFailedAttempts(user);

        if (twoFactorService.isTwoFactorEnabled()) {
            String sessionToken = twoFactorService.generateAndSendTwoFactorCode(user);
            loginAttemptService.recordLoginAttempt(email, ipAddress, userAgent, false, "Aguardando 2FA", true);

            log.info("2FA obrigatório para usuário: {}", email);
            return createBaseLoginResponse()
                    .success(false)
                    .message("Código de verificação enviado para seu email")
                    .requiresTwoFactor(true)
                    .sessionToken(sessionToken)
                    .username(user.getUsername())
                    .build();
        }

        
        String token = jwtUtil.generateToken(user.getUsername());
        loginAttemptService.recordSuccessfulLogin(email, ipAddress, userAgent, false);
        securityLoggingService.logLoginSuccess(email, httpRequest);
        auditoriaService.loginSucesso(email, ipAddress, userAgent, false);

        log.info("Login bem-sucedido para usuário: {}", email);
        return createBaseLoginResponse()
                .success(true)
                .message("Login realizado com sucesso")
                .token(token)
                .username(user.getUsername())
                .requiresTwoFactor(false)
                .build();
    }

        private LoginResponse handleAccountLocked(String email, HttpServletRequest httpRequest) {
        boolean activeCaptcha = appConfig.isActiveCaptcha();
        if (activeCaptcha) {
            Captcha generator = new Captcha();
            Captcha.CaptchaData captcha;
            try {
                captcha = generator.generateCaptcha();
            } catch (Exception e) {
                log.error("Erro ao gerar captcha", e);
                return createBaseLoginResponse()
                        .success(false)
                        .message("Erro interno ao gerar captcha")
                        .requiresTwoFactor(false)
                        .build();
            }

            httpRequest.getSession().setAttribute("captcha", captcha.getText());

            return createBaseLoginResponse()
                    .success(false)
                    .message("Responda o captcha para fazer login novamente.")
                    .captchaImage(captcha.getImage())
                    .requiresTwoFactor(false)
                    .build();
        }

        return createBaseLoginResponse()
                .success(false)
                .message("Conta temporariamente bloqueada. Tente novamente mais tarde.")
                .requiresTwoFactor(false)
                .build();
    }

    public LoginResponse verifyTwoFactor(TwoFactorRequest request, String ipAddress, String userAgent) {
        log.info("Verificação 2FA para usuário: {}", request.getEmail());

        try {
            
            User user = userValidationService.validateAndGetUser(request.getEmail(), ipAddress, userAgent);

            
            twoFactorService.validateTwoFactorCode(user, request.getCode(), request.getSessionToken());

            
            String token = jwtUtil.generateToken(user.getUsername());
            loginAttemptService.recordSuccessfulLogin(request.getEmail(), ipAddress, userAgent, true);
            auditoriaService.loginSucesso(request.getEmail(), ipAddress, userAgent, true);

            log.info("Login completo com 2FA para usuário: {}", request.getEmail());
            return createBaseLoginResponse()
                    .success(true)
                    .message("Login realizado com sucesso")
                    .token(token)
                    .username(user.getUsername())
                    .requiresTwoFactor(false)
                    .build();

        } catch (UserValidationService.UserNotFoundException e) {
            loginAttemptService.recordFailedLogin(request.getEmail(), ipAddress, userAgent, "Usuário não encontrado");
            return createBaseLoginResponse()
                    .success(false)
                    .message("Credenciais inválidas")
                    .requiresTwoFactor(false)
                    .build();

        } catch (TwoFactorService.TwoFactorValidationException e) {
            loginAttemptService.recordFailedLogin(request.getEmail(), ipAddress, userAgent, e.getMessage());
            return createBaseLoginResponse()
                    .success(false)
                    .message(e.getMessage())
                    .requiresTwoFactor(false)
                    .build();

        } catch (Exception e) {
            log.error("Erro inesperado durante verificação 2FA", e);
            loginAttemptService.recordFailedLogin(request.getEmail(), ipAddress, userAgent, "Erro interno");
            return createBaseLoginResponse()
                    .success(false)
                    .message("Erro interno do servidor")
                    .requiresTwoFactor(false)
                    .build();
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
