package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class FraudModelService {

    @Value("${fraud.model.enabled:false}")
    private boolean modelEnabled;

    @Value("${fraud.model.url:}")
    private String modelUrl;

    @Value("${fraud.model.timeout-ms:2000}")
    private int timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public int getAdditionalRiskScore(String sha256, String riskLevelHint) {
        if (!modelEnabled || modelUrl == null || modelUrl.isBlank()) {
            return 0;
        }
        try {
            String payload = "{\"sha256\":\"" + sha256 + "\",\"riskHint\":\"" + sanitize(riskLevelHint) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractRiskScore(response.body());
            }
            return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int extractRiskScore(String body) {
        if (body == null) {
            return 0;
        }
        int idx = body.indexOf("\"score\"");
        if (idx < 0) {
            return 0;
        }
        int colon = body.indexOf(':', idx);
        if (colon < 0) {
            return 0;
        }
        int end = colon + 1;
        while (end < body.length() && (body.charAt(end) == ' ' || body.charAt(end) == '"')) {
            end++;
        }
        int start = end;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        if (start == end) {
            return 0;
        }
        int score = Integer.parseInt(body.substring(start, end));
        return Math.max(0, Math.min(25, score));
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "");
    }
}
