package com.example.ecommerce.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // We define expected audience and issuer. You can override these in application.yml from the config server
    @Value("${jwt.audience:ecommerce-microservices}")
    private String expectedAudience;

    @Value("${jwt.issuer:ecommerce-auth-server}")
    private String expectedIssuer;

    public void validateToken(final String token) {
        Jwts.parserBuilder()
            .setSigningKey(getSignKey())
            // 1. Expiration is checked AUTOMATICALLY by JJWT parseClaimsJws. It throws ExpiredJwtException.
            // 2. We check the Audience (aud) to ensure the token is meant for this project
            .requireAudience(expectedAudience)
            // 3. We check the Issuer (iss) to ensure it was created by the trusted auth server
            .requireIssuer(expectedIssuer)
            .build()
            .parseClaimsJws(token);
    }

    public String extractUserId(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // Method to extract scopes/roles from the token
    public String extractScopes(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
        // Typically scopes are in the "scope" or "scopes" or "roles" claim. Adjust as necessary.
        return claims.get("scope", String.class); 
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
