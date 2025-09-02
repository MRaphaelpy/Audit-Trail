package com.mraphaelpy.auditoria.service.audit;

import com.mraphaelpy.auditoria.entity.audit.Auditoria;
import com.mraphaelpy.auditoria.entity.audit.EventoSeguranca;
import com.mraphaelpy.auditoria.repository.audit.AuditoriaRepository;
import com.mraphaelpy.auditoria.repository.audit.EventoSegurancaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditoriaService {
    
    private final AuditoriaRepository auditoriaRepository;
    private final EventoSegurancaRepository eventoSegurancaRepository;
    
    public void registrarEvento(String usuario, String acao, Map<String, Object> detalhes, 
                               String ip, String userAgent, Auditoria.NivelAuditoria nivel,
                               String origem, String recurso) {
        try {
            Auditoria auditoria = Auditoria.builder()
                    .usuario(usuario)
                    .acao(acao)
                    .detalhes(detalhes)
                    .ip(ip)
                    .userAgent(userAgent)
                    .nivel(nivel)
                    .origem(origem)
                    .recurso(recurso)
                    .criadoEm(LocalDateTime.now())
                    .build();
            
            auditoriaRepository.save(auditoria);
            
            
            log.info("AUDIT_EVENT: usuario={}, acao={}, nivel={}, ip={}, recurso={}", 
                    usuario, acao, nivel, ip, recurso);
            
        } catch (Exception e) {
            log.error("Erro ao registrar evento de auditoria", e);
        }
    }
    
    public void registrarEventoSeguranca(String tipoEvento, String usuario, String ipOrigem, 
                                       String userAgent, EventoSeguranca.Resultado resultado,
                                       Map<String, Object> detalhes, EventoSeguranca.Gravidade gravidade,
                                       String acaoTomada) {
        try {
            EventoSeguranca evento = EventoSeguranca.builder()
                    .tipoEvento(tipoEvento)
                    .usuario(usuario)
                    .ipOrigem(ipOrigem)
                    .userAgent(userAgent)
                    .resultado(resultado)
                    .detalhesSeguranca(detalhes)
                    .gravidade(gravidade)
                    .acaoTomada(acaoTomada)
                    .timestampEvento(LocalDateTime.now())
                    .build();
            
            eventoSegurancaRepository.save(evento);
            
            
            log.warn("SECURITY_EVENT: tipo={}, usuario={}, resultado={}, gravidade={}, ip={}", 
                    tipoEvento, usuario, resultado, gravidade, ipOrigem);
            
        } catch (Exception e) {
            log.error("Erro ao registrar evento de segurança", e);
        }
    }
    
    
    public void loginSucesso(String usuario, String ip, String userAgent, boolean twoFactor) {
        Map<String, Object> detalhes = Map.of(
                "metodo_auth", "password",
                "two_factor", twoFactor,
                "timestamp", LocalDateTime.now()
        );
        
        registrarEvento(usuario, "LOGIN_SUCCESS", detalhes, ip, userAgent, 
                       Auditoria.NivelAuditoria.INFO, "aplicacao", "authentication");
        
        registrarEventoSeguranca("LOGIN_SUCCESS", usuario, ip, userAgent, 
                               EventoSeguranca.Resultado.SUCCESS, detalhes, 
                               EventoSeguranca.Gravidade.LOW, null);
    }
    
    public void loginFalha(String usuario, String ip, String userAgent, String motivo, int tentativa) {
        Map<String, Object> detalhes = Map.of(
                "motivo", motivo,
                "tentativa", tentativa,
                "timestamp", LocalDateTime.now()
        );
        
        registrarEvento(usuario, "LOGIN_FAILURE", detalhes, ip, userAgent, 
                       Auditoria.NivelAuditoria.WARN, "aplicacao", "authentication");
        
        EventoSeguranca.Gravidade gravidade = tentativa >= 3 ? 
                EventoSeguranca.Gravidade.HIGH : EventoSeguranca.Gravidade.MEDIUM;
        
        registrarEventoSeguranca("LOGIN_FAILED", usuario, ip, userAgent, 
                               EventoSeguranca.Resultado.FAILURE, detalhes, 
                               gravidade, null);
    }
    
    public void contaBloqueada(String usuario, String ip, String userAgent, int tentativas) {
        Map<String, Object> detalhes = Map.of(
                "tentativas_totais", tentativas,
                "acao", "conta_bloqueada",
                "timestamp", LocalDateTime.now()
        );
        
        registrarEvento(usuario, "ACCOUNT_LOCKED", detalhes, ip, userAgent, 
                       Auditoria.NivelAuditoria.ERROR, "sistema", "security");
        
        registrarEventoSeguranca("ACCOUNT_LOCKED", usuario, ip, userAgent, 
                               EventoSeguranca.Resultado.BLOCKED, detalhes, 
                               EventoSeguranca.Gravidade.CRITICAL, "Conta bloqueada por múltiplas tentativas");
    }
    
    public void accessoNegado(String usuario, String recurso, String ip, String userAgent, String motivo) {
        Map<String, Object> detalhes = Map.of(
                "recurso_solicitado", recurso,
                "motivo_negacao", motivo,
                "timestamp", LocalDateTime.now()
        );
        
        registrarEvento(usuario, "ACCESS_DENIED", detalhes, ip, userAgent, 
                       Auditoria.NivelAuditoria.WARN, "sistema", recurso);
        
        registrarEventoSeguranca("PERMISSION_DENIED", usuario, ip, userAgent, 
                               EventoSeguranca.Resultado.BLOCKED, detalhes, 
                               EventoSeguranca.Gravidade.HIGH, null);
    }
    
    public void operacaoCritica(String usuario, String operacao, String recurso, 
                               Map<String, Object> detalhes, String ip, String userAgent) {
        registrarEvento(usuario, operacao, detalhes, ip, userAgent, 
                       Auditoria.NivelAuditoria.CRITICAL, "aplicacao", recurso);
        
        registrarEventoSeguranca("CRITICAL_OPERATION", usuario, ip, userAgent, 
                               EventoSeguranca.Resultado.SUCCESS, detalhes, 
                               EventoSeguranca.Gravidade.HIGH, "Operação crítica executada");
    }
}
