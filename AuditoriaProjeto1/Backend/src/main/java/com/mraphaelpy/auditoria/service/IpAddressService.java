package com.mraphaelpy.auditoria.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IpAddressService {

        public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest é null, retornando IP padrão");
            return "unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {

            String clientIp = xForwardedFor.split(",")[0].trim();
            log.debug("IP extraído de X-Forwarded-For: {}", clientIp);
            return clientIp;
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            log.debug("IP extraído de X-Real-IP: {}", xRealIP);
            return xRealIP;
        }

        String xOriginalForwardedFor = request.getHeader("X-Original-Forwarded-For");
        if (xOriginalForwardedFor != null && !xOriginalForwardedFor.isEmpty()
                && !"unknown".equalsIgnoreCase(xOriginalForwardedFor)) {
            log.debug("IP extraído de X-Original-Forwarded-For: {}", xOriginalForwardedFor);
            return xOriginalForwardedFor;
        }

        String remoteAddr = request.getRemoteAddr();
        log.debug("IP extraído de RemoteAddr: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "unknown";
    }

        public boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            return false;
        }

        return ipAddress.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
                ipAddress.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }
}
