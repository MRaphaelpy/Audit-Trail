package com.mraphaelpy.auditoria.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mraphaelpy.auditoria.entity.logs.ApplicationLog;
import com.mraphaelpy.auditoria.repository.logs.ApplicationLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private ApplicationLogRepository applicationLogRepository;
    private ObjectMapper objectMapper;
    private final Executor executor = Executors.newFixedThreadPool(5);

    public DatabaseLogAppender(ApplicationLogRepository applicationLogRepository) {
        this.applicationLogRepository = applicationLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void append(ILoggingEvent event) {
        
        CompletableFuture.runAsync(() -> {
            try {
                ApplicationLog log = buildApplicationLog(event);
                applicationLogRepository.save(log);
            } catch (Exception e) {
                
                System.err.println("Erro ao salvar log no banco: " + e.getMessage());
            }
        }, executor);
    }

    private ApplicationLog buildApplicationLog(ILoggingEvent event) {
        ApplicationLog.ApplicationLogBuilder builder = ApplicationLog.builder()
                .timestamp(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(event.getTimeStamp()),
                        ZoneId.systemDefault()))
                .level(event.getLevel().toString())
                .loggerName(event.getLoggerName())
                .threadName(event.getThreadName())
                .message(event.getFormattedMessage());

        
        if (event.getThrowableProxy() != null) {
            ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
            builder.exceptionMessage(throwableProxy.getMessage())
                    .exceptionClass(throwableProxy.getClassName())
                    .stackTrace(buildStackTrace(throwableProxy));
        }

        
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            try {
                builder.mdcData(objectMapper.writeValueAsString(mdcPropertyMap));

                
                builder.requestId(mdcPropertyMap.get("requestId"))
                        .userId(mdcPropertyMap.get("userId"))
                        .sessionId(mdcPropertyMap.get("sessionId"))
                        .ipAddress(mdcPropertyMap.get("ipAddress"))
                        .userAgent(mdcPropertyMap.get("userAgent"))
                        .requestUri(mdcPropertyMap.get("requestUri"))
                        .httpMethod(mdcPropertyMap.get("httpMethod"));
            } catch (Exception e) {
                System.err.println("Erro ao serializar MDC: " + e.getMessage());
            }
        }

        return builder.build();
    }

    private String buildStackTrace(ThrowableProxy throwableProxy) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append("\n");

        for (int i = 0; i < throwableProxy.getStackTraceElementProxyArray().length; i++) {
            sb.append("\tat ").append(throwableProxy.getStackTraceElementProxyArray()[i].toString()).append("\n");
        }

        return sb.toString();
    }
}
