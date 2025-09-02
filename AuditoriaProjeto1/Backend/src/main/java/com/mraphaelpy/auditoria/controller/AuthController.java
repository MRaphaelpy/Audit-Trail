package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.dto.request.LoginRequest;
import com.mraphaelpy.auditoria.dto.request.TwoFactorRequest;
import com.mraphaelpy.auditoria.dto.response.ApiResponse;
import com.mraphaelpy.auditoria.dto.response.LoginResponse;
import com.mraphaelpy.auditoria.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AppConfig appConfig;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            LoginResponse response = authService.authenticate(request, ipAddress, userAgent);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Erro durante login", e);
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .message("Erro interno do servidor")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<LoginResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            LoginResponse response = authService.verifyTwoFactor(request, ipAddress, userAgent);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Erro durante verificação 2FA", e);
            LoginResponse errorResponse = LoginResponse.builder()
                    .success(false)
                    .message("Erro interno do servidor")
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout realizado com sucesso"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth controller está funcionando"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse> test() {
        return ResponseEntity.ok(ApiResponse.success("Endpoint de teste funcionando"));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAuthConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("twoFactorEnabled", appConfig.isTwoFactorEnabled());
        config.put("twoFactorCodeExpiryMinutes", appConfig.getTwoFactorCodeExpiryMinutes());
        config.put("accountLockEnabled", appConfig.isAccountLockEnabled());
        config.put("accountLockMaxAttempts", appConfig.getAccountLockMaxAttempts());
        config.put("accountLockDurationMinutes", appConfig.getAccountLockDurationMinutes());
        config.put("infiniteAttemptsEnabled", appConfig.isInfiniteAttemptsEnabled());

        return ResponseEntity.ok(config);
    }

    @PostMapping("/config/two-factor")
    public ResponseEntity<ApiResponse> toggleTwoFactor(@RequestParam boolean enabled) {
        try {
            appConfig.setTwoFactorEnabled(enabled);
            String message = enabled ? "2FA ativado com sucesso" : "2FA desativado com sucesso";
            log.info("Configuração 2FA alterada para: {}", enabled);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (Exception e) {
            log.error("Erro ao alterar configuração 2FA", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erro ao alterar configuração"));
        }
    }

    @PostMapping("/config/infinite-attempts")
    public ResponseEntity<ApiResponse> toggleInfiniteAttempts(@RequestParam boolean enabled) {
        try {
            appConfig.setInfiniteAttemptsEnabled(enabled);
            String message = enabled ? "Tentativas infinitas ativadas" : "Tentativas infinitas desativadas";
            log.info("Configuração de tentativas infinitas alterada para: {}", enabled);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (Exception e) {
            log.error("Erro ao alterar configuração de tentativas infinitas", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erro ao alterar configuração"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
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
