package com.codex.identity_verifier.service;

import com.codex.identity_verifier.model.VerificationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DynamoDBService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;
    @Value("${aws.dynamodb.table-name:IdentityVerifications}")
    private String tableName;

    @Autowired
    public DynamoDBService(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Creates the verification records table if it doesn't exist
     */
    public void createTableIfNotExists() {
        try {
            // Check if table exists
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
                    
        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, create it
            createTable();
        }
    }

    /**
     * Creates the DynamoDB table for verification records
     */
    private void createTable() {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("createdAt")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("createdAt")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDbClient.createTable(createTableRequest);

        // Wait for table to be created
        try {
            dynamoDbClient.waiter().waitUntilTableExists(
                    DescribeTableRequest.builder().tableName(tableName).build());
        } catch (Exception ex) {
            // Handle exception if needed
        }
    }

    /**
     * Saves a verification record to DynamoDB
     * @param verificationRecord The record to save
     */
    public void saveVerificationRecord(VerificationRecord verificationRecord) {
        // Set up the table
        DynamoDbTable<VerificationRecord> verificationTable = enhancedClient
                .table(tableName, TableSchema.fromBean(VerificationRecord.class));

        // Set the ID if not already set
        if (verificationRecord.getId() == null || verificationRecord.getId().isEmpty()) {
            verificationRecord.setId(UUID.randomUUID().toString());
        }

        // Set timestamps
        Instant now = Instant.now();
        verificationRecord.setCreatedAt(now);
        verificationRecord.setUpdatedAt(now);

        // Save the item; create table lazily if missing.
        try {
            verificationTable.putItem(verificationRecord);
        } catch (ResourceNotFoundException ex) {
            createTableIfNotExists();
            verificationTable.putItem(verificationRecord);
        }
    }

    /**
     * Retrieves a verification record by ID
     * @param id The ID of the record to retrieve
     * @return The verification record or null if not found
     */
    public VerificationRecord getVerificationRecordById(String id) {
        DynamoDbTable<VerificationRecord> verificationTable = enhancedClient
                .table(tableName, TableSchema.fromBean(VerificationRecord.class));

        // For demo purposes, we'll scan for the record since we don't have the exact timestamp
        // In production, you'd want to use a GSI or store the timestamp separately
        try {
            return verificationTable.scan()
                .items()
                .stream()
                .filter(record -> id.equals(record.getId()))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets recent verification records
     * @param limit Number of records to return
     * @return List of verification records
     */
    public List<VerificationRecord> getRecentVerifications(int limit) {
        DynamoDbTable<VerificationRecord> verificationTable = enhancedClient
                .table(tableName, TableSchema.fromBean(VerificationRecord.class));

        // For this implementation, we'll use a scan operation
        // In a production environment, you'd want to use a query with a GSI
        try {
            java.util.List<VerificationRecord> result = new java.util.ArrayList<>();
            verificationTable.scan(r -> r.limit(limit)).items().iterator().forEachRemaining(result::add);
            return result;
        } catch (Exception e) {
            // Return empty list if there's an error
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Updates a verification record in DynamoDB
     * @param verificationRecord The record to update
     */
    public void updateVerificationRecord(VerificationRecord verificationRecord) {
        DynamoDbTable<VerificationRecord> verificationTable = enhancedClient
                .table(tableName, TableSchema.fromBean(VerificationRecord.class));

        verificationRecord.setUpdatedAt(Instant.now());
        verificationTable.updateItem(verificationRecord);
    }

    /**
     * Deletes a verification record by ID
     * @param id The ID of the record to delete
     */
    public void deleteVerificationRecord(String id, Instant createdAt) {
        DynamoDbTable<VerificationRecord> verificationTable = enhancedClient
                .table(tableName, TableSchema.fromBean(VerificationRecord.class));

        verificationTable.deleteItem(Key.builder()
                .partitionValue(id)
                .sortValue(createdAt.toString())
                .build());
    }
}
