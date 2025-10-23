package com.ada.proj.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        // HS256 requires a key size >= 256 bits (32 bytes). Ensure secret length is sufficient.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String uuid, String role) {
        return generate(uuid, role, accessExpirationMs);
    }

    public String generateRefreshToken(String uuid, String role) {
        return generate(uuid, role, refreshExpirationMs);
    }

    private String generate(String uuid, String role, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(uuid)
                .addClaims(Map.of("role", role))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ttlMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public String getUuid(String token) {
        return parse(token).getBody().getSubject();
    }

    public String getRole(String token) {
        Object role = parse(token).getBody().get("role");
        return role == null ? null : role.toString();
    }
}
