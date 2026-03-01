package com.codex.identity_verifier.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class VerificationRecord {

    private String id;
    private String fileName;
    private String fileHash;
    private String s3Bucket;
    private String s3Key;
    private Boolean s3ObjectDeleted;

    private String riskLevel;
    private Integer riskScore;

    private List<String> explanation;

    private ExtractedData extractedData;

    private Double faceMatchConfidence;
    private Boolean isTampered;

    private Instant createdAt;
    private Instant updatedAt;

    // Partition Key
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Sort Key
    @DynamoDbSortKey
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Nested DynamoDB object
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @DynamoDbBean
    public static class ExtractedData {

        private String name;
        private String idNumber;
        private String dob;
        private String address;
        private String expiryDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdNumber() {
            return idNumber;
        }

        public void setIdNumber(String idNumber) {
            this.idNumber = idNumber;
        }

        public String getDob() {
            return dob;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }
    }
}
