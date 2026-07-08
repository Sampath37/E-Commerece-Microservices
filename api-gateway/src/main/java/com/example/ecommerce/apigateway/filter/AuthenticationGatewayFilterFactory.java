package com.example.ecommerce.apigateway.filter;

import com.example.ecommerce.apigateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationGatewayFilterFactory(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.requiresAuth(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }
                
                try {
                    jwtUtil.validateToken(authHeader);
                    String userId = jwtUtil.extractUserId(authHeader);
                    String scopes = jwtUtil.extractScopes(authHeader);
                    
                    // Mutate the request to add the user id and scopes header
                    exchange = exchange.mutate()
                        .request(builder -> {
                            builder.header("X-Auth-User-Id", userId);
                            if (scopes != null) {
                                builder.header("X-Auth-User-Scopes", scopes);
                            }
                        })
                        .build();
                        
                } catch (Exception e) {
                    System.out.println("Invalid access...! Reason: " + e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}
