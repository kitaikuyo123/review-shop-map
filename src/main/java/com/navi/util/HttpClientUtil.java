package com.navi.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HttpClientUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("HTTP GET failed: " + url, e);
        }
    }

    public static String post(String url, String jsonBody) {
        return post(url, jsonBody, null);
    }

    public static String post(String url, String jsonBody, String bearerToken) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            if (bearerToken != null && !bearerToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            HttpResponse<byte[]> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("HTTP POST failed: " + url, e);
        }
    }
}
