package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.dto.request.CaptchaRequest;
import com.mraphaelpy.auditoria.dto.response.CaptchaResponse;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.entity.audit.Auditoria;
import com.mraphaelpy.auditoria.repository.UserRepository;
import com.mraphaelpy.auditoria.service.audit.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaService {

    private final UserRepository userRepository;
    private final AuditoriaService auditoriaService;
    private final IpAddressService ipAddressService;

    @Transactional
    public CaptchaResponse verifyCaptcha(CaptchaRequest request, HttpServletRequest httpRequest) {
        try {

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return CaptchaResponse.failure("Email é obrigatório");
            }

            if (request.getCaptcha() == null || request.getCaptcha().trim().isEmpty()) {
                return CaptchaResponse.failure("Código do captcha é obrigatório");
            }

            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                log.warn("Tentativa de verificação de captcha para email inexistente: {}", request.getEmail());
                auditoriaService.registrarEvento(
                        request.getEmail(),
                        "CAPTCHA_USER_NOT_FOUND",
                        Map.of(
                                "email", request.getEmail(),
                                "reason", "user_not_found"),
                        ipAddressService.getClientIpAddress(httpRequest),
                        httpRequest.getHeader("User-Agent"),
                        Auditoria.NivelAuditoria.WARN,
                        "aplicacao",
                        "authentication");
                return CaptchaResponse.failure("Usuário não encontrado");
            }

            User user = userOpt.get();

            String captchaSession = (String) httpRequest.getSession().getAttribute("captcha");
            if (captchaSession == null) {
                log.warn("Captcha não encontrado na sessão para usuário: {}", user.getEmail());
                auditoriaService.registrarEvento(
                        user.getEmail(),
                        "CAPTCHA_SESSION_MISSING",
                        Map.of(
                                "email", user.getEmail(),
                                "reason", "session_expired"),
                        ipAddressService.getClientIpAddress(httpRequest),
                        httpRequest.getHeader("User-Agent"),
                        Auditoria.NivelAuditoria.WARN,
                        "aplicacao",
                        "authentication");
                return CaptchaResponse.failure("Sessão de captcha expirada ou não encontrada");
            }

            if (!captchaSession.equalsIgnoreCase(request.getCaptcha().trim())) {
                log.warn("Captcha incorreto fornecido para usuário: {}", user.getEmail());
                auditoriaService.registrarEvento(
                        user.getEmail(),
                        "CAPTCHA_INCORRECT",
                        Map.of(
                                "email", user.getEmail(),
                                "provided_captcha", request.getCaptcha(),
                                "result", "failure"),
                        ipAddressService.getClientIpAddress(httpRequest),
                        httpRequest.getHeader("User-Agent"),
                        Auditoria.NivelAuditoria.WARN,
                        "aplicacao",
                        "authentication");
                return CaptchaResponse.failure("Código do captcha incorreto");
            }

            unlockUserAccount(user);

            httpRequest.getSession().removeAttribute("captcha");

            auditoriaService.registrarEvento(
                    user.getEmail(),
                    "CAPTCHA_SUCCESS",
                    Map.of(
                            "email", user.getEmail(),
                            "result", "success",
                            "action", "account_unlocked"),
                    ipAddressService.getClientIpAddress(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    Auditoria.NivelAuditoria.INFO,
                    "aplicacao",
                    "authentication");

            log.info("Captcha verificado com sucesso - conta desbloqueada: {}", user.getEmail());
            return CaptchaResponse.success("Captcha verificado com sucesso. Conta desbloqueada.");

        } catch (Exception e) {
            log.error("Erro interno ao verificar captcha para email: {}", request.getEmail(), e);
            auditoriaService.registrarEvento(
                    request.getEmail(),
                    "CAPTCHA_ERROR",
                    Map.of(
                            "email", request.getEmail(),
                            "error", e.getMessage(),
                            "result", "error"),
                    ipAddressService.getClientIpAddress(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    Auditoria.NivelAuditoria.ERROR,
                    "aplicacao",
                    "authentication");
            return CaptchaResponse.error("Erro interno do servidor");
        }
    }

    private void unlockUserAccount(User user) {
        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
        log.info("Conta desbloqueada automaticamente: {}", user.getUsername());
    }
}
