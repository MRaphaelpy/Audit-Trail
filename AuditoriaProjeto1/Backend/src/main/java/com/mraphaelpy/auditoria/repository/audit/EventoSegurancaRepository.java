package com.mraphaelpy.auditoria.repository.audit;

import com.mraphaelpy.auditoria.entity.audit.EventoSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long> {
    
    List<EventoSeguranca> findByUsuarioAndTimestampEventoAfter(String usuario, LocalDateTime dataInicio);
    
    List<EventoSeguranca> findByTipoEventoAndTimestampEventoAfter(String tipoEvento, LocalDateTime dataInicio);
    
    List<EventoSeguranca> findByIpOrigemAndTimestampEventoAfter(String ipOrigem, LocalDateTime dataInicio);
    
    List<EventoSeguranca> findByGravidadeAndTimestampEventoAfter(EventoSeguranca.Gravidade gravidade, LocalDateTime dataInicio);
    
    @Query("SELECT COUNT(e) FROM EventoSeguranca e WHERE e.tipoEvento LIKE '%LOGIN%' AND e.resultado = 'FAILURE' AND e.timestampEvento >= :dataInicio")
    Long contarTentativasLoginFalhas(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT COUNT(e) FROM EventoSeguranca e WHERE e.tipoEvento LIKE '%LOGIN%' AND e.resultado = 'SUCCESS' AND e.timestampEvento >= :dataInicio")
    Long contarLoginsComSucesso(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("""
        SELECT DATE_TRUNC('minute', e.timestampEvento) as minuto,
               COUNT(CASE WHEN e.resultado = 'SUCCESS' THEN 1 END) as sucessos,
               COUNT(CASE WHEN e.resultado = 'FAILURE' THEN 1 END) as falhas
        FROM EventoSeguranca e 
        WHERE e.tipoEvento LIKE '%LOGIN%' 
          AND e.timestampEvento >= :dataInicio 
        GROUP BY DATE_TRUNC('minute', e.timestampEvento) 
        ORDER BY minuto DESC
        """)
    List<Object[]> obterTentativasLoginPorMinuto(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT e.ipOrigem, COUNT(e) as total FROM EventoSeguranca e WHERE e.timestampEvento >= :dataInicio AND e.ipOrigem IS NOT NULL GROUP BY e.ipOrigem ORDER BY COUNT(e) DESC")
    List<Object[]> encontrarIPsComMaisTentativas(@Param("dataInicio") LocalDateTime dataInicio);
    
    @Query("SELECT e FROM EventoSeguranca e WHERE e.gravidade = 'CRITICAL' AND e.timestampEvento >= :dataInicio ORDER BY e.timestampEvento DESC")
    List<EventoSeguranca> encontrarEventosCriticos(@Param("dataInicio") LocalDateTime dataInicio);
}
