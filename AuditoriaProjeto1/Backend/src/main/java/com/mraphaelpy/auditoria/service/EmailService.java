package com.mraphaelpy.auditoria.service;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service

@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    
    public void sendTwoFactorCode(String toEmail, String username, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Código de Verificação - Sistema de Login");
            message.setText(buildTwoFactorEmailBody(username, code));
            
            mailSender.send(message);
            log.info("Código 2FA enviado para o email: {}", toEmail);
        } catch (Exception e) {
            log.error("Erro ao enviar email de 2FA para {}: {}", toEmail, e.getMessage(), e);
            
        }
    }
    
    private String buildTwoFactorEmailBody(String username, String code) {
        return String.format(
            """
            Olá %s,
            
            Seu código de verificação para autenticação em dois fatores é:
            
            %s
            
            Este código expira em 5 minutos.
            
            Se você não solicitou este código, ignore este email.
            
            Atenciosamente,
            Sistema de Segurança
            """,
            username, code
        );
    }
    
    
    public void mockSendTwoFactorCode(String toEmail, String username, String code) {
        log.info("=== MOCK EMAIL SERVICE ===");
        log.info("Para: {}", toEmail);
        log.info("Usuário: {}", username);
        log.info("Código 2FA: {}", code);
        log.info("========================");
    }
}
