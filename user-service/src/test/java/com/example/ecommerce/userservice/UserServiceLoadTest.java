package com.example.ecommerce.userservice;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserServiceLoadTest {

    private static final String API_URL = "http://localhost:8081/api/users";
    private static final int TOTAL_REQUESTS = 100;

    @Test
    public void testCreate100UsersConcurrently() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("Starting UserService Load Test: " + TOTAL_REQUESTS + " users");

        for (int i = 1; i <= TOTAL_REQUESTS; i++) {
            // Generate a unique ID each time to ensure new users
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String name = "TestUser_" + uniqueId;
            String email = "testuser_" + uniqueId + "@example.com";
            String address = "123 Random Street, City " + uniqueId;
            
            String jsonPayload = String.format(
                    "{\"name\": \"%s\", \"email\": \"%s\", \"address\": \"%s\"}",
                    name, email, address
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            successCount.incrementAndGet();
                            System.out.println("[SUCCESS] Created User: " + name);
                        } else {
                            System.err.println("[FAILED] User: " + name + " - " + response.body());
                        }
                    });

            futures.add(future);
        }

        // Wait for all requests
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        System.out.println("Total Successfully Created: " + successCount.get());
        assertEquals(TOTAL_REQUESTS, successCount.get(), "All 100 users should be successfully created.");
    }
}
