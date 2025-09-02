package com.mraphaelpy.auditoria.entity.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "logs_estruturados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEstruturado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private UUID uuid;

    @Column(name = "timestamp_log", nullable = false)
    @Builder.Default
    private LocalDateTime timestampLog = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelLog nivel;

    @Column(nullable = false, length = 255)
    private String logger;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false, length = 100)
    private String thread;

    @Column(nullable = false, length = 255)
    private String classe;

    @Column(nullable = false, length = 100)
    private String metodo;

    private Integer linha;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> contexto;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(length = 100)
    private String servico;

    @Column(length = 20)
    private String versao;

    @Column(length = 50)
    private String ambiente;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (timestampLog == null) {
            timestampLog = LocalDateTime.now();
        }
    }

    public enum NivelLog {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
}
