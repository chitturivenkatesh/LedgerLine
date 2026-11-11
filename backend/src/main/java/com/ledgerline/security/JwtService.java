package com.ledgerline.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-minutes}") long minutes) {
        byte[] bytes = Decoders.BASE64.decode(base64Secret);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttl = Duration.ofMinutes(minutes);
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder().subject(email).claim("role", role)
                .issuedAt(now).expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key).compact();
    }

    /** Throws JwtException if the token is invalid, tampered with or expired. */
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public long ttlSeconds() { return ttl.toSeconds(); }
}
