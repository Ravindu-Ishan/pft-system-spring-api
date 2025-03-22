package org.spring.pftsystem.utility;

import io.jsonwebtoken.*;
import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.repository.SystemSettingsRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Log
@Component
public class JwtUtil {

    @Value("${spring.jwt.secret}")
    private String secret;

    private final SystemSettingsRepo systemSettingsRepo;

    public JwtUtil(SystemSettingsRepo systemSettingsRepo) {
        this.systemSettingsRepo = systemSettingsRepo;

    }


    // Generate the JWT token
    public String generateToken(String id, Map<String, Object> claims) {

        SystemSettings settings = systemSettingsRepo.findFirstByOrderByIdAsc();
        int expiryTimeInMinutes = settings.getJWTExpirationTime();

        if(expiryTimeInMinutes == 0){
            log.warning("JWT Expiry time is not set in the system settings. Defaulting to 60 minutes.");
            expiryTimeInMinutes = 60;
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (long) expiryTimeInMinutes * 60 * 1000)) // 1 hour expiry
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // Parse claims from the token
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate the token
    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
