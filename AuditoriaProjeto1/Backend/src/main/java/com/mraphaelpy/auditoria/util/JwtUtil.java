package com.mraphaelpy.auditoria.util;

import com.mraphaelpy.auditoria.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        String secret = jwtProperties.getSecret();
        byte[] keyBytes;

        try {
            // Tenta decodificar como Base64
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // Se não for Base64 válido, usa os bytes brutos
            keyBytes = secret.getBytes();
        }

        // Verifica se a chave tem o tamanho adequado (32 bytes/256 bits)
        if (keyBytes.length < 32) {
            log.warn("JWT secret key is too short! Creating a stronger key by extending the current one.");
            byte[] strongKey = new byte[32];

            // Copia os bytes da chave original
            System.arraycopy(keyBytes, 0, strongKey, 0, Math.min(keyBytes.length, 32));

            // Preenche o resto copiando os bytes iniciais
            for (int i = keyBytes.length; i < 32; i++) {
                strongKey[i] = keyBytes[i % keyBytes.length];
            }

            keyBytes = strongKey;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, "ACCESS");
    }

    public String generateToken(String username, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getExpiration(), ChronoUnit.MILLIS);

        return Jwts.builder()
                .setSubject(username)
                .claim("type", tokenType)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateSessionToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(10, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(username)
                .claim("type", "SESSION")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    public boolean isSessionToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "SESSION".equals(tokenType);
        } catch (Exception e) {
            log.error("Error checking session token", e);
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}