package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.config.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CaptchaController {
    private final AppConfig appConfig;

    @PostMapping("/verify-captcha")
    public String verifyCaptcha(@RequestParam("captcha") String captcha) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        String captchaSession = (String) request.getSession().getAttribute("captcha");

        if (captchaSession == null) {
            log.warn("Captcha não encontrado na sessão.");
            return "captcha-session-missing";
        }
        if (captchaSession.equalsIgnoreCase(captcha.trim())) {
            appConfig.setAccountLockMaxAttempts(2);
            return "captcha-success";
        } else {
            return "captcha-failure";
        }
    }
}
