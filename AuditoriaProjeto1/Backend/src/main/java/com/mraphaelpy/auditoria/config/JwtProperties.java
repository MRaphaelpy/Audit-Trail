package com.mraphaelpy.auditoria.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    @Value("${app.jwt.secret:defaultSecretKey}")
    private String secret;

    private long expiration = 86400000;
}
