package com.payxpress.security;

import com.payxpress.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private static final long ACCESS_TOKEN_MINUTES = 15;

    public String issueAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("phone", user.getPhone())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(ACCESS_TOKEN_MINUTES, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
