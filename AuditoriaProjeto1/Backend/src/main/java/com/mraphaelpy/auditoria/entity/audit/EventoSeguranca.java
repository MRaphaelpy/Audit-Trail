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

@Entity
@Table(name = "eventos_seguranca")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoSeguranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp_evento", nullable = false)
    @Builder.Default
    private LocalDateTime timestampEvento = LocalDateTime.now();

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(nullable = false, length = 255)
    private String usuario;

    @Column(name = "ip_origem", nullable = false, length = 45)
    private String ipOrigem;

    @Column(name = "user_agent", nullable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Resultado resultado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalhes_seguranca", nullable = false, columnDefinition = "json")
    private Map<String, Object> detalhesSeguranca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Gravidade gravidade = Gravidade.MEDIUM;

    @Column(name = "acao_tomada", length = 255)
    private String acaoTomada;

    @PrePersist
    public void prePersist() {
        if (timestampEvento == null) {
            timestampEvento = LocalDateTime.now();
        }
    }

    public enum Resultado {
        SUCCESS, FAILURE, BLOCKED
    }

    public enum Gravidade {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
