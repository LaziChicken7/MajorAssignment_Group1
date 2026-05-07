package com.auction.util;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiService {
    public static String BASE_URL = "http://localhost:8080/auction";

    // ĐÃ NÂNG CẤP: Bật tính năng theo dõi chuyển hướng (Follow Redirects)
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static final Gson gson = new Gson();

    public static <T> CompletableFuture<HttpResponse<String>> postAsync(String endpoint, T body) {
        String jsonBody = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("User-Agent", "JavaFX-Client/1.0") // BẮT BUỘC: Khai báo danh tính để không bị Ngrok coi là bot
                .header("ngrok-skip-browser-warning", "true")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> getAsync(String endpoint) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("User-Agent", "JavaFX-Client/1.0")
                .header("ngrok-skip-browser-warning", "true")
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static <T> CompletableFuture<HttpResponse<String>> putAsync(String endpoint, T body) {
        String jsonBody = body != null ? gson.toJson(body) : "";
        HttpRequest.BodyPublisher publisher = body != null ?
                HttpRequest.BodyPublishers.ofString(jsonBody) :
                HttpRequest.BodyPublishers.noBody();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("User-Agent", "JavaFX-Client/1.0")
                .header("ngrok-skip-browser-warning", "true")
                .PUT(publisher)
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<HttpResponse<String>> deleteAsync(String endpoint) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("User-Agent", "JavaFX-Client/1.0")
                .header("ngrok-skip-browser-warning", "true")
                .DELETE()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}