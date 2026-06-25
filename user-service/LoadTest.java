import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final String API_URL = "http://localhost:8081/api/users";
    private static final int TOTAL_REQUESTS = 100;

    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        System.out.println("Starting Load Test: Sending " + TOTAL_REQUESTS + " concurrent requests to create users...");
        
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= TOTAL_REQUESTS; i++) {
            final int userId = i;
            String name = "TestUser" + userId;
            String email = "testuser" + userId + "@example.com";
            String address = "123 Test Street, City " + userId;
            
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
                            System.out.println("[SUCCESS] Created User " + userId + " - Status: " + response.statusCode());
                        } else {
                            failCount.incrementAndGet();
                            System.out.println("[FAILED] User " + userId + " - Status: " + response.statusCode() + " | Body: " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        failCount.incrementAndGet();
                        System.out.println("[ERROR] Request for User " + userId + " failed: " + ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all concurrent requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("=================================================");
        System.out.println("Load Test Completed in " + duration + " ms");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failCount.get());
        System.out.println("=================================================");
    }
}
