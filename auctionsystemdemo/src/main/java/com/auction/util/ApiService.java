package com.auction.util;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiService {
    // Địa chỉ của Spring Boot
    private static final String BASE_URL = "http://localhost:8080/auction";
    private static final HttpClient client = HttpClient.newHttpClient();
    public static final Gson gson = new Gson();

    // Hàm dùng để gửi dữ liệu đi (POST)
    public static <T> CompletableFuture<HttpResponse<String>> postAsync(String endpoint, T body) {
        String jsonBody = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}