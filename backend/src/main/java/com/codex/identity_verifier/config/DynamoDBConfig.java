package com.codex.identity_verifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Profile("aws")
public class DynamoDBConfig {
    
    @Value("${aws.dynamodb.region:us-east-1}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider credentialsProvider) {
        String dynamoRegion = System.getenv("AWS_DYNAMODB_REGION");
        if (dynamoRegion == null || dynamoRegion.isEmpty()) {
            dynamoRegion = System.getProperty("aws.dynamodb.region", region);
        }
        return DynamoDbClient.builder()
                .region(Region.of(dynamoRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}