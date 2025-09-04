package com.mraphaelpy.auditoria.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
@Data
public class AppConfig {

    private final Environment environment;

    @Value("${app.two-factor.enabled:true}")
    private boolean twoFactorEnabled;

    @Value("${app.two-factor.code-expiry-minutes:10}")
    private int twoFactorCodeExpiryMinutes;

    @Value("${app.account-lock.enabled:true}")
    private boolean accountLockEnabled;

    @Value("${app.account-lock.max-attempts:3}")
    private int accountLockMaxAttempts;

    @Value("${app.account-lock.duration-minutes:30}")
    private int accountLockDurationMinutes;

    @Value("${app.infinite-attempts.enabled:false}")
    private boolean infiniteAttemptsEnabled;

    @Value("${app.active-captcha:false}")
    private boolean activeCaptcha;

    public AppConfig(Environment environment) {
        this.environment = environment;
        loadEnvironmentVariables();
    }

    private void loadEnvironmentVariables() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .filename(".env")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

        } catch (Exception e) {
            System.out.println("Aviso: Não foi possível carregar o arquivo .env. Usando configurações padrão.");
        }
    }

    @PostConstruct
    public void init() {
        String twoFactorEnabledEnv = System.getProperty("TWO_FACTOR_ENABLED");
        if (twoFactorEnabledEnv != null) {
            this.twoFactorEnabled = Boolean.parseBoolean(twoFactorEnabledEnv);
        }

        String twoFactorExpiryEnv = System.getProperty("TWO_FACTOR_CODE_EXPIRY_MINUTES");
        if (twoFactorExpiryEnv != null) {
            try {
                this.twoFactorCodeExpiryMinutes = Integer.parseInt(twoFactorExpiryEnv);
            } catch (NumberFormatException e) {
            }
        }

        String infiniteAttemptsEnabledEnv = System.getProperty("INFINITE_ATTEMPTS_ENABLED");
        if (infiniteAttemptsEnabledEnv != null) {
            this.infiniteAttemptsEnabled = Boolean.parseBoolean(infiniteAttemptsEnabledEnv);
        }
        String activeCaptchaEnv = System.getProperty("ACTIVE_CAPTCHA");
        if (activeCaptchaEnv != null) {
            this.activeCaptcha = Boolean.parseBoolean(activeCaptchaEnv);
        }
    }
}
