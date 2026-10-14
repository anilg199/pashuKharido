package com.pashumandi.milk.security;

import com.pashumandi.milk.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private Key signingKey;

    @PostConstruct
    public void init() {
        if (jwtConfig.getSecret() == null || jwtConfig.getSecret().trim().isEmpty()) {
            throw new IllegalStateException("JWT signing secret (jwt.secret / JWT_SECRET) is not configured!");
        }

        byte[] keyBytes;
        try {
            // Attempt Base64 decode first (standard PashuMandi Auth convention)
            keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        } catch (Exception e) {
            // Fallback to UTF-8 bytes if not standard Base64
            keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        }

        // Guarantee at least 256 bits (32 bytes) for HMAC-SHA256
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token");
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid");
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT token has no subject");
        }
        return Long.parseLong(subject.trim());
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role.toUpperCase() : "BUYER";
    }

    public AuthenticatedUser getAuthenticatedUser(String token) {
        Long userId = extractUserId(token);
        String role = extractRole(token);
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        return AuthenticatedUser.builder()
                .userId(userId)
                .role(role)
                .authorities(authorities)
                .build();
    }

    /**
     * Helper method to generate access tokens for testing purposes.
     */
    public String generateTokenForTest(Long userId, String role, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
