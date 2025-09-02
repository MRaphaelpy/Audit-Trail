package com.mraphaelpy.auditoria.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {
    
    private boolean enableSecurityFeatures = true;
    private int maxLoginAttempts = 3;
    private long captchaTimeout = 300000; 
    private long twoFactorCodeExpiration = 300000; 
}
