package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.dto.request.LoginRequest;
import com.mraphaelpy.auditoria.dto.request.TwoFactorRequest;
import com.mraphaelpy.auditoria.dto.response.ApiResponse;
import com.mraphaelpy.auditoria.dto.response.LoginResponse;
import com.mraphaelpy.auditoria.service.AuthService;
import com.mraphaelpy.auditoria.service.IpAddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AppConfig appConfig;
    private final IpAddressService ipAddressService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = ipAddressService.getClientIpAddress(httpRequest);
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
            String ipAddress = ipAddressService.getClientIpAddress(httpRequest);
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
}
