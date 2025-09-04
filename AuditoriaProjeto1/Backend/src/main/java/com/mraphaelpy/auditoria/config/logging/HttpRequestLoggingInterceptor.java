package com.mraphaelpy.auditoria.config.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mraphaelpy.auditoria.entity.logs.HttpRequestLog;
import com.mraphaelpy.auditoria.repository.logs.HttpRequestLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpRequestLoggingInterceptor implements HandlerInterceptor {

    private final HttpRequestLogRepository httpRequestLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        
        String requestId = UUID.randomUUID().toString();

        
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("ipAddress", getClientIpAddress(request));
        MDC.put("userAgent", request.getHeader("User-Agent"));

        
        request.setAttribute("startTime", System.currentTimeMillis());
        request.setAttribute("requestId", requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        try {
            
            Long startTime = (Long) request.getAttribute("startTime");
            Long processingTime = startTime != null ? System.currentTimeMillis() - startTime : null;

            
            CompletableFuture.runAsync(() -> saveHttpRequestLog(request, response, processingTime, ex));

        } catch (Exception e) {
            log.error("Erro ao registrar log da requisição HTTP", e);
        } finally {
            
            MDC.clear();
        }
    }

    private void saveHttpRequestLog(HttpServletRequest request, HttpServletResponse response,
                                    Long processingTime, Exception exception) {
        try {
            String requestId = (String) request.getAttribute("requestId");

            
            String userId = getCurrentUserId(request);
            if (userId == null) {
                userId = "anonymous";
            }

            HttpRequestLog httpLog = HttpRequestLog.builder()
                    .requestId(requestId)
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .queryString(request.getQueryString())
                    .headers(getRequestHeaders(request))
                    .requestBody(getRequestBody(request))
                    .responseStatus(response.getStatus())
                    .responseBody(getResponseBody(response))
                    .responseHeaders(getResponseHeaders(response))
                    .processingTimeMs(processingTime)
                    .userId(userId) 
                    .sessionId(request.getSession(false) != null ? request.getSession().getId() : null)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .timestamp(LocalDateTime.now())
                    .build();

            httpRequestLogRepository.save(httpLog);

        } catch (Exception e) {
            log.error("Erro ao salvar log HTTP no banco", e);
        }
    }

    private String getRequestHeaders(HttpServletRequest request) {
        try {
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();

            while (headerNames != null && headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }

            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String getResponseHeaders(HttpServletResponse response) {
        try {
            Map<String, String> headers = new HashMap<>();

            for (String headerName : response.getHeaderNames()) {
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, response.getHeader(headerName));
                }
            }

            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String getRequestBody(HttpServletRequest request) {
        try {
            if (request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String body = new String(content, StandardCharsets.UTF_8);
                    
                    return body.length() > 10000 ? body.substring(0, 10000) + "..." : body;
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao capturar body da requisição", e);
        }
        return null;
    }

    private String getResponseBody(HttpServletResponse response) {
        try {
            if (response instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String body = new String(content, StandardCharsets.UTF_8);
                    
                    return body.length() > 10000 ? body.substring(0, 10000) + "..." : body;
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao capturar body da resposta", e);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private String getCurrentUserId(HttpServletRequest request) {
        
        
        return MDC.get("userId");
    }

    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null)
            return false;
        String lower = headerName.toLowerCase();
        return lower.contains("authorization") ||
                lower.contains("password") ||
                lower.contains("token") ||
                lower.contains("secret") ||
                lower.contains("cookie");
    }
}
