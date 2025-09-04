package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.dto.request.CaptchaRequest;
import com.mraphaelpy.auditoria.dto.response.CaptchaResponse;
import com.mraphaelpy.auditoria.service.CaptchaService;
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
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CaptchaController {

    private final CaptchaService captchaService;

    @PostMapping("/verify-captcha")
    public ResponseEntity<CaptchaResponse> verifyCaptcha(
            @Valid @RequestBody CaptchaRequest captchaRequest,
            HttpServletRequest request) {

        log.debug("Recebida requisição de verificação de captcha para email: {}", captchaRequest.getEmail());

        CaptchaResponse response = captchaService.verifyCaptcha(captchaRequest, request);

        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("não encontrado") ||
                response.getMessage().contains("expirada")) {
            return ResponseEntity.badRequest().body(response);
        } else if (response.getMessage().contains("Erro interno")) {
            return ResponseEntity.internalServerError().body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
