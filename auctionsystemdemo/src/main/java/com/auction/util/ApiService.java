package com.auction.util;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiService {
    // Đổi link này nếu Spring Boot của bạn dùng port khác hoặc có context-path
    private static final String BASE_URL = "http://localhost:8080/auction";
    private static final HttpClient client = HttpClient.newHttpClient();
    public static final Gson gson = new Gson();

    // 1. GỬI DỮ LIỆU LÊN (Dùng cho Đăng nhập, Đăng ký, Nạp tiền...)
    public static <T> CompletableFuture<HttpResponse<String>> postAsync(String endpoint, T body) {
        String jsonBody = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // 2. LẤY DỮ LIỆU VỀ (Dùng cho xem Profile, xem Danh sách sản phẩm...)
    public static CompletableFuture<HttpResponse<String>> getAsync(String endpoint) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // 3. CẬP NHẬT DỮ LIỆU (Dùng cho Đổi mật khẩu, Cập nhật Profile...)
    public static <T> CompletableFuture<HttpResponse<String>> putAsync(String endpoint, T body) {
        String jsonBody = body != null ? gson.toJson(body) : "";
        HttpRequest.BodyPublisher publisher = body != null ?
                HttpRequest.BodyPublishers.ofString(jsonBody) :
                HttpRequest.BodyPublishers.noBody();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .PUT(publisher)
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // 4. XÓA DỮ LIỆU
    public static CompletableFuture<HttpResponse<String>> deleteAsync(String endpoint) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .DELETE()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}