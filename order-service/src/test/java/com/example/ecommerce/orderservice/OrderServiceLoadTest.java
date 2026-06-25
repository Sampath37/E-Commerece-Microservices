package com.example.ecommerce.orderservice;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderServiceLoadTest {

    private static final String USER_API_URL = "http://localhost:8081/api/users";
    private static final String ORDER_API_URL = "http://localhost:8082/api/orders";
    private static final int TOTAL_REQUESTS = 100;

    @Test
    public void testCreateOrdersFor100NewUsersConcurrently() throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        List<String[]> createdUsers = new ArrayList<>();
        
        System.out.println("Step 1: Synchronously creating 100 new users to get fresh user IDs...");
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String address = "Order St " + uniqueId;
            String jsonPayload = String.format(
                    "{\"name\": \"OrderTester_%s\", \"email\": \"order_%s@example.com\", \"address\": \"%s\"}",
                    uniqueId, uniqueId, address
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USER_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "User creation should succeed");

            // Extract the generated userId from the response
            Pattern pattern = Pattern.compile("\"userId\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response.body());
            if (matcher.find()) {
                String userId = matcher.group(1);
                createdUsers.add(new String[]{userId, address});
            }
        }
        
        System.out.println("Successfully created " + createdUsers.size() + " users.");
        assertEquals(TOTAL_REQUESTS, createdUsers.size(), "Should have successfully parsed 100 userIds");

        System.out.println("Step 2: Sending 100 concurrent order requests for those new users...");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < createdUsers.size(); i++) {
            String userId = createdUsers.get(i)[0];
            String address = createdUsers.get(i)[1];
            String productCode = "PROD-" + (1000 + (int)(Math.random() * 9000));
            int quantity = 1;

            String jsonPayload = String.format(
                    "{\"userId\": \"%s\", \"productCode\": \"%s\", \"quantity\": %d, \"address\": \"%s\"}",
                    userId, productCode, quantity, address
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ORDER_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            successCount.incrementAndGet();
                            System.out.println("[SUCCESS] Created Order for User " + userId);
                        } else {
                            System.err.println("[FAILED] Order for User " + userId + " - " + response.body());
                        }
                    });

            futures.add(future);
        }

        // Wait for all order requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        System.out.println("Total Successfully Created Orders: " + successCount.get());
        assertEquals(TOTAL_REQUESTS, successCount.get(), "All 100 order requests should succeed");
    }
}
