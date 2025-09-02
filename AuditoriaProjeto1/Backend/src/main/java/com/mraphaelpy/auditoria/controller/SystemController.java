package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.config.SecurityProperties;
import com.mraphaelpy.auditoria.dto.response.ApiResponse;
import com.mraphaelpy.auditoria.entity.LoginAttempt;
import com.mraphaelpy.auditoria.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemController {
    
    private final SecurityProperties securityProperties;
    private final LoginAttemptRepository loginAttemptRepository;
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", System.currentTimeMillis());
        healthData.put("securityFeaturesEnabled", securityProperties.isEnableSecurityFeatures());
        healthData.put("maxLoginAttempts", securityProperties.getMaxLoginAttempts());
        
        return ResponseEntity.ok(ApiResponse.success("Sistema funcionando", healthData));
    }
    
    @GetMapping("/config")
    public ResponseEntity<ApiResponse> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enableSecurityFeatures", securityProperties.isEnableSecurityFeatures());
        config.put("maxLoginAttempts", securityProperties.getMaxLoginAttempts());
        config.put("captchaTimeout", securityProperties.getCaptchaTimeout());
        config.put("twoFactorCodeExpiration", securityProperties.getTwoFactorCodeExpiration());
        
        return ResponseEntity.ok(ApiResponse.success("Configurações do sistema", config));
    }
    
    @GetMapping("/login-attempts")
    public ResponseEntity<ApiResponse> getRecentLoginAttempts(@RequestParam(defaultValue = "10") int limit) {
        List<LoginAttempt> attempts = loginAttemptRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "attemptTime"))
        ).getContent();
        
        return ResponseEntity.ok(ApiResponse.success("Tentativas de login recentes", attempts));
    }
    
    @GetMapping("/login-attempts/{username}")
    public ResponseEntity<ApiResponse> getUserLoginAttempts(@PathVariable String username, 
                                                          @RequestParam(defaultValue = "10") int limit) {
        List<LoginAttempt> attempts = loginAttemptRepository.findByUsernameOrderByAttemptTimeDesc(username)
                .stream()
                .limit(limit)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success("Tentativas de login do usuário", attempts));
    }
}
