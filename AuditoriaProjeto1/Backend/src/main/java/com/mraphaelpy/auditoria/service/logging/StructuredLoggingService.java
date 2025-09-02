package com.mraphaelpy.auditoria.service.logging;

import com.mraphaelpy.auditoria.entity.audit.LogEstruturado;
import com.mraphaelpy.auditoria.repository.audit.LogEstruturadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StructuredLoggingService {

    private final LogEstruturadoRepository logEstruturadoRepository;

    public void logEvent(LogEstruturado.NivelLog nivel, String logger, String mensagem,
            String thread, String classe, String metodo, Integer linha,
            Map<String, Object> contexto, String stackTrace) {

        CompletableFuture.runAsync(() -> {
            try {
                LogEstruturado logEvent = LogEstruturado.builder()
                        .timestampLog(LocalDateTime.now())
                        .nivel(nivel)
                        .logger(logger != null ? logger : "application")
                        .mensagem(mensagem != null ? mensagem : "Log estruturado")
                        .thread(thread != null ? thread : Thread.currentThread().getName())
                        .classe(classe != null ? classe : "unknown")
                        .metodo(metodo != null ? metodo : "unknown")
                        .linha(linha)
                        .contexto(contexto)
                        .stackTrace(stackTrace)
                        .servico("auditoria-app")
                        .versao("1.0.0")
                        .ambiente("development")
                        .build();

                logEstruturadoRepository.save(logEvent);

            } catch (Exception e) {
                log.error("Erro ao salvar log estruturado: {}", e.getMessage());
            }
        });
    }

    public void logInfo(String logger, String mensagem, Map<String, Object> contexto) {
        logEvent(LogEstruturado.NivelLog.INFO, logger, mensagem,
                Thread.currentThread().getName(), null, null, null, contexto, null);
    }

    public void logWarn(String logger, String mensagem, Map<String, Object> contexto) {
        logEvent(LogEstruturado.NivelLog.WARN, logger, mensagem,
                Thread.currentThread().getName(), null, null, null, contexto, null);
    }

    public void logError(String logger, String mensagem, Map<String, Object> contexto, String stackTrace) {
        logEvent(LogEstruturado.NivelLog.ERROR, logger, mensagem,
                Thread.currentThread().getName(), null, null, null, contexto, stackTrace);
    }

    public void logApplicationStart(String version, String environment) {
        Map<String, Object> contexto = Map.of(
                "event_type", "application_start",
                "version", version,
                "environment", environment,
                "startup_time", LocalDateTime.now());

        logInfo("system", "Aplicação iniciada", contexto);
    }

    public void logApplicationError(String errorClass, String errorMessage, String stackTrace) {
        Map<String, Object> contexto = Map.of(
                "event_type", "application_error",
                "error_class", errorClass,
                "error_message", errorMessage,
                "timestamp", LocalDateTime.now());

        logError("system", "Erro na aplicação", contexto, stackTrace);
    }

    public void logDatabaseOperation(String operation, String table, Map<String, Object> details) {
        Map<String, Object> contexto = Map.of(
                "event_type", "database_operation",
                "operation", operation,
                "table", table,
                "details", details,
                "timestamp", LocalDateTime.now());

        logInfo("database", "Operação no banco de dados", contexto);
    }

    public void logPerformanceMetrics(String operation, long executionTimeMs, Map<String, Object> metrics) {
        Map<String, Object> contexto = Map.of(
                "event_type", "performance_metrics",
                "operation", operation,
                "execution_time_ms", executionTimeMs,
                "metrics", metrics,
                "timestamp", LocalDateTime.now());

        logInfo("performance", "Métricas de performance", contexto);
    }
}
