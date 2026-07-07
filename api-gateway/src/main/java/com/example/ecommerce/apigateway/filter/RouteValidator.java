package com.example.ecommerce.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/users/login",
            "/eureka"
    );

    // If we want change-password to be secured by JWT, we shouldn't add it to openApiEndpoints.
    // Wait, the user has to be logged in to change password. But the token will be validated.
    // So /api/users/change-password should NOT be here. 
    // Wait, what about POST /api/users (signup)?
    
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
                    
    // Note: We need a custom check for POST /api/users since GET /api/users should be secured.
    public boolean requiresAuth(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        
        if (path.equals("/api/users") && method.equals("POST")) {
            return false; // Signup is open
        }
        
        return isSecured.test(request);
    }
}
