package com.mraphaelpy.auditoria.config.audit;

import com.mraphaelpy.auditoria.entity.audit.Auditoria;
import com.mraphaelpy.auditoria.service.audit.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {
    
    private final AuditoriaService auditoriaService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("audit_start_time", System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, Exception ex) throws Exception {
        
        try {
            String usuario = obterUsuarioAtual(request);
            String acao = determinarAcao(request);
            String ip = obterIpCliente(request);
            String userAgent = request.getHeader("User-Agent");
            Auditoria.NivelAuditoria nivel = determinarNivel(response.getStatus(), ex);
            
            Long startTime = (Long) request.getAttribute("audit_start_time");
            long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;
            
            Map<String, Object> detalhes = new HashMap<>();
            detalhes.put("metodo_http", request.getMethod());
            detalhes.put("url", request.getRequestURL().toString());
            detalhes.put("status_code", response.getStatus());
            detalhes.put("tempo_execucao_ms", executionTime);
            detalhes.put("timestamp", LocalDateTime.now());
            
            if (request.getQueryString() != null) {
                detalhes.put("query_params", request.getQueryString());
            }
            
            if (ex != null) {
                detalhes.put("excecao", ex.getClass().getSimpleName());
                detalhes.put("erro_mensagem", ex.getMessage());
                nivel = Auditoria.NivelAuditoria.ERROR;
            }
            
            if (deveAuditar(request, response)) {
                auditoriaService.registrarEvento(
                        usuario, acao, detalhes, ip, userAgent, nivel, "interceptor", 
                        obterRecurso(request)
                );
            }
            
        } catch (Exception e) {
            log.error("Erro no interceptor de auditoria", e);
        }
    }
    
    private String obterUsuarioAtual(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        
        return "anonymous@" + obterIpCliente(request);
    }
    
    private String determinarAcao(HttpServletRequest request) {
        String metodo = request.getMethod();
        String path = request.getRequestURI();
        
        if (path.contains("/auth/login")) return "LOGIN_ATTEMPT";
        if (path.contains("/auth/logout")) return "LOGOUT";
        if (path.contains("/users/register")) return "USER_REGISTRATION";
        if (path.contains("/admin")) return "ADMIN_ACCESS";
        
        return switch (metodo) {
            case "GET" -> "READ_" + extrairRecursoDaUrl(path);
            case "POST" -> "CREATE_" + extrairRecursoDaUrl(path);
            case "PUT" -> "UPDATE_" + extrairRecursoDaUrl(path);
            case "DELETE" -> "DELETE_" + extrairRecursoDaUrl(path);
            default -> "HTTP_" + metodo + "_" + extrairRecursoDaUrl(path);
        };
    }
    
    private String extrairRecursoDaUrl(String path) {
        String[] partes = path.split("/");
        if (partes.length > 2) {
            return partes[2].toUpperCase();
        }
        return "UNKNOWN";
    }
    
    private Auditoria.NivelAuditoria determinarNivel(int statusCode, Exception ex) {
        if (ex != null) return Auditoria.NivelAuditoria.ERROR;
        
        if (statusCode >= 500) return Auditoria.NivelAuditoria.ERROR;
        if (statusCode >= 400) return Auditoria.NivelAuditoria.WARN;
        if (statusCode >= 300) return Auditoria.NivelAuditoria.INFO;
        return Auditoria.NivelAuditoria.INFO;
    }
    
    private String obterIpCliente(HttpServletRequest request) {
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
    
    private String obterRecurso(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/")) {
            return path.substring(4);
        }
        return path;
    }
    
    private boolean deveAuditar(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        
        if (path.matches(".*\\.(css|js|png|jpg|gif|ico|woff|ttf)$")) {
            return false;
        }
        
        if (path.contains("/health") && response.getStatus() < 400) {
            return false;
        }
        
        if ("OPTIONS".equals(request.getMethod())) {
            return false;
        }
        
        if (path.contains("/auth") || path.contains("/admin") ||
            path.contains("/users") || response.getStatus() >= 400) {
            return true;
        }
        
        return "POST".equals(request.getMethod()) ||
               "PUT".equals(request.getMethod()) || 
               "DELETE".equals(request.getMethod());
    }
}
