package me.dhiya.hr.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import me.dhiya.hr.config.JwtProperties;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.services.JwtPayload;
import me.dhiya.hr.services.JwtService;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtServiceImpl(SecretKey secretKey, JwtProperties jwtProperties) {
        this.secretKey = secretKey;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String generateToken(EmployeeEntity employee) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(employee.getId())
                .claim("email", employee.getEmail())
                .claim("role", employee.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getAccessTokenExpiration())))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public JwtPayload extractPayload(String token) {
        Claims claims = parseClaims(token);
        return new JwtPayload(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("role", String.class)
        );
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
