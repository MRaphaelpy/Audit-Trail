package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.config.AppConfig;
import com.mraphaelpy.auditoria.dto.request.CaptchaRequest;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CaptchaController {
    //private final AppConfig appConfig;
    private final UserRepository userRepository;

    @PostMapping("/verify-captcha")
    public String verifyCaptcha(@RequestBody CaptchaRequest captchaRequest) {
        Optional<User>  userOpt = userRepository.findByEmail(captchaRequest.getEmail());
        User user = userOpt.get();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        String captchaSession = (String) request.getSession().getAttribute("captcha");

        if (captchaSession == null) {
            log.warn("Captcha não encontrado na sessão.");
            return "captcha-session-missing";
        }
        if (captchaSession.equalsIgnoreCase(captchaRequest.getCaptcha().trim())) {
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setFailedAttempts(0);
            userRepository.save(user);
            log.info("Conta desbloqueada automaticamente: {}", user.getUsername());
            //appConfig.setAccountLockMaxAttempts(2);
            return "captcha-success";
        } else {
            return "captcha-failure";
        }
    }
}
