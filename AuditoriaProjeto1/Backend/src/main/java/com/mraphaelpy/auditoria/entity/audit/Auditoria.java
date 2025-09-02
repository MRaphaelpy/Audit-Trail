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
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private UUID uuid;
    
    @Column(nullable = false, length = 255)
    private String usuario;
    
    @Column(nullable = false, length = 255)
    private String acao;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> detalhes;
    
    @Column(length = 45)
    private String ip;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private NivelAuditoria nivel = NivelAuditoria.INFO;
    
    @Column(length = 100)
    private String origem;
    
    @Column(length = 255)
    private String recurso;
    
    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
    
    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
    
    public enum NivelAuditoria {
        INFO, WARN, ERROR, DEBUG, CRITICAL
    }
}
