package cn.har01d.survey.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.har01d.survey.service.SystemConfigService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long defaultExpirationMs;
    private final SystemConfigService configService;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            SystemConfigService configService) {
        this.defaultExpirationMs = expirationMs;
        this.configService = configService;
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(resolveSecret(secret)));
    }

    private String resolveSecret(String defaultSecret) {
        String stored = configService.get(SystemConfigService.JWT_SECRET);
        if (!stored.isEmpty()) {
            return stored;
        }
        String secret = defaultSecret;
        if (secret == null || secret.isEmpty()) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            secret = Base64.getEncoder().encodeToString(bytes);
            log.info("Generated random JWT secret");
        }
        configService.set(SystemConfigService.JWT_SECRET, secret);
        return secret;
    }

    private long getExpirationMs() {
        String val = configService.get(SystemConfigService.JWT_EXPIRATION_MS);
        if (!val.isEmpty()) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultExpirationMs;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
