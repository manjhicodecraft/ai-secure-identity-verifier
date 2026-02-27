package com.codex.identity_verifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
@Profile("aws")
public class AwsConfig {
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    @Value("${aws.access.key-id:#{null}}")
    private String accessKeyId;
    
    @Value("${aws.secret.access-key:#{null}}")
    private String secretAccessKey;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKeyId != null && secretAccessKey != null && !accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            return StaticCredentialsProvider.create(credentials);
        }
        
        // Fall back to default credentials provider (IAM roles, etc.)
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        String s3Region = System.getenv("AWS_S3_BUCKET_REGION");
        if (s3Region == null || s3Region.isEmpty()) {
            s3Region = System.getProperty("aws.s3.bucket-region", region);
        }
        return S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public RekognitionClient rekognitionClient(AwsCredentialsProvider credentialsProvider) {
        String rekognitionRegion = System.getenv("AWS_REKOGNITION_REGION");
        if (rekognitionRegion == null || rekognitionRegion.isEmpty()) {
            rekognitionRegion = System.getProperty("aws.rekognition.region", region);
        }
        return RekognitionClient.builder()
                .region(Region.of(rekognitionRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public TextractClient textractClient(AwsCredentialsProvider credentialsProvider) {
        String textractRegion = System.getenv("AWS_TEXTRACT_REGION");
        if (textractRegion == null || textractRegion.isEmpty()) {
            textractRegion = System.getProperty("aws.textract.region", region);
        }
        return TextractClient.builder()
                .region(Region.of(textractRegion))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}