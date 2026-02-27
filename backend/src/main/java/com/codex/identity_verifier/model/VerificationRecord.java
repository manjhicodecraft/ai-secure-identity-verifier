package com.codex.identity_verifier.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class VerificationRecord {

    private String id;
    private String fileName;
    private String s3Bucket;
    private String s3Key;
    private String riskLevel;
    private Integer riskScore;
    private String[] explanation;
    private ExtractedData extractedData;
    private Double faceMatchConfidence;
    private Boolean isTampered;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbBean
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedData {
        private String name;
        private String idNumber;
        private String dob;
        private String address;
        private String expiryDate;
    }
}