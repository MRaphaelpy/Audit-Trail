package com.mraphaelpy.auditoria.repository.audit;

import com.mraphaelpy.auditoria.entity.audit.LogEstruturado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEstruturadoRepository extends JpaRepository<LogEstruturado, Long> {

    List<LogEstruturado> findByNivelOrderByTimestampLogDesc(LogEstruturado.NivelLog nivel);

    List<LogEstruturado> findByLoggerOrderByTimestampLogDesc(String logger);

    @Query("SELECT l FROM LogEstruturado l WHERE l.timestampLog BETWEEN :inicio AND :fim ORDER BY l.timestampLog DESC")
    Page<LogEstruturado> findByTimestampLogBetween(@Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    @Query("SELECT l FROM LogEstruturado l WHERE l.nivel = :nivel AND l.timestampLog BETWEEN :inicio AND :fim ORDER BY l.timestampLog DESC")
    List<LogEstruturado> findByNivelAndTimestampLogBetween(@Param("nivel") LogEstruturado.NivelLog nivel,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(l) FROM LogEstruturado l WHERE l.nivel = :nivel")
    Long countByNivel(@Param("nivel") LogEstruturado.NivelLog nivel);

    @Query("SELECT COUNT(l) FROM LogEstruturado l WHERE l.timestampLog BETWEEN :inicio AND :fim")
    Long countByTimestampLogBetween(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT l FROM LogEstruturado l WHERE l.classe LIKE %:classe% ORDER BY l.timestampLog DESC")
    List<LogEstruturado> findByClasseContaining(@Param("classe") String classe);

    @Query("SELECT l FROM LogEstruturado l WHERE l.servico = :servico ORDER BY l.timestampLog DESC")
    List<LogEstruturado> findByServico(@Param("servico") String servico);
}
