package com.dAdK.dubAI.util;

import com.dAdK.dubAI.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String SECRET;
    @Value("${app.jwt.expirationSeconds}")
    private int EXPIRATION;

    public String generateToken(User user) {
        Date tokenExpiryInterval = new Date(System.currentTimeMillis() + (EXPIRATION * 1000L));

        return Jwts.builder()
                .setSubject(user.getId())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(tokenExpiryInterval)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token, User user) {
        return extractUserId(token).equals(user.getId()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}