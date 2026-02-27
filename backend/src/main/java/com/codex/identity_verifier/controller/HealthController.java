package com.codex.identity_verifier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;

    @Autowired
    public HealthController(S3Client s3Client, DynamoDbClient dynamoDbClient) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "AI Secure Identity Verifier");
        healthInfo.put("timestamp", System.currentTimeMillis());

        // Test S3 connectivity
        try {
            s3Client.listBuckets();
            healthInfo.put("s3Status", "CONNECTED");
        } catch (Exception e) {
            healthInfo.put("s3Status", "ERROR: " + e.getMessage());
        }

        // Test DynamoDB connectivity
        try {
            dynamoDbClient.listTables();
            healthInfo.put("dynamodbStatus", "CONNECTED");
        } catch (Exception e) {
            healthInfo.put("dynamodbStatus", "ERROR: " + e.getMessage());
        }

        return ResponseEntity.ok(healthInfo);
    }
}