import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderLoadTest {

    private static final String USER_API_URL = "http://localhost:8081/api/users";
    private static final String ORDER_API_URL = "http://localhost:8082/api/orders";

    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        System.out.println("Fetching users from " + USER_API_URL + "...");
        
        HttpRequest getUsersRequest = HttpRequest.newBuilder()
                .uri(URI.create(USER_API_URL))
                .GET()
                .build();

        String usersJson = "";
        try {
            HttpResponse<String> response = client.send(getUsersRequest, HttpResponse.BodyHandlers.ofString());
            usersJson = response.body();
        } catch (Exception e) {
            System.err.println("Failed to fetch users: " + e.getMessage());
            return;
        }

        // Extremely simple regex to parse userId and address
        List<String[]> users = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"userId\":\"([^\"]+)\".*?\"address\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(usersJson);
        
        while (matcher.find()) {
            users.add(new String[]{matcher.group(1), matcher.group(2)});
            if (users.size() >= 100) {
                break;
            }
        }

        if (users.isEmpty()) {
            System.out.println("No users found to create orders for!");
            return;
        }

        System.out.println("Found " + users.size() + " users. Starting Order Load Test...");

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < users.size(); i++) {
            String userId = users.get(i)[0];
            String address = users.get(i)[1];
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

            int finalI = i;
            CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            successCount.incrementAndGet();
                            System.out.println("[SUCCESS] Created Order for User " + userId + " - Status: " + response.statusCode());
                        } else {
                            failCount.incrementAndGet();
                            System.out.println("[FAILED] Order for User " + userId + " - Status: " + response.statusCode() + " | Body: " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        failCount.incrementAndGet();
                        System.out.println("[ERROR] Request for Order " + finalI + " failed: " + ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all concurrent requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("=================================================");
        System.out.println("Order Load Test Completed in " + duration + " ms");
        System.out.println("Total Requests: " + users.size());
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failCount.get());
        System.out.println("=================================================");
    }
}
