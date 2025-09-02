package com.mraphaelpy.auditoria.repository.audit;

import com.mraphaelpy.auditoria.entity.audit.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    List<Auditoria> findByUsuarioAndCriadoEmAfter(String usuario, LocalDateTime dataInicio);
    
    List<Auditoria> findByAcaoAndCriadoEmAfter(String acao, LocalDateTime dataInicio);
    
    List<Auditoria> findByNivelAndCriadoEmAfter(Auditoria.NivelAuditoria nivel, LocalDateTime dataInicio);
    
    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.usuario = :usuario AND a.criadoEm >= :dataInicio")
    Long contarEventosPorUsuario(@Param("usuario") String usuario, @Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT a.acao, COUNT(a) as total FROM Auditoria a WHERE a.criadoEm >= :dataInicio GROUP BY a.acao ORDER BY COUNT(a) DESC")
    List<Object[]> encontrarAcoesMaisExecutadas(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT a.ip, COUNT(a) as total FROM Auditoria a WHERE a.criadoEm >= :dataInicio AND a.ip IS NOT NULL GROUP BY a.ip ORDER BY COUNT(a) DESC")
    List<Object[]> encontrarIPsMaisAtivos(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("""
        SELECT DATE_TRUNC('hour', a.criadoEm) as hora, COUNT(a) as total 
        FROM Auditoria a 
        WHERE a.criadoEm >= :dataInicio 
        GROUP BY DATE_TRUNC('hour', a.criadoEm) 
        ORDER BY hora DESC
        """)
    List<Object[]> obterEventosPorHora(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT a FROM Auditoria a WHERE a.nivel IN ('ERROR', 'CRITICAL') AND a.criadoEm >= :dataInicio ORDER BY a.criadoEm DESC")
    List<Auditoria> encontrarEventosCriticos(@Param("dataInicio") LocalDateTime dataInicio);
}
